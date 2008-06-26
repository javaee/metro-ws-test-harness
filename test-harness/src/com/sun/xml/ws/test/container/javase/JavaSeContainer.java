/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.test.container.javase;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.container.jelly.EndpointInfoBean;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.tool.WsTool;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.ServerSocket;
import java.util.*;

/**
 * Container to deploy Java SE endpoints using java.xml.ws.Endpoint API
 *
 * @author Ken Hofsass
 * @author Jitendra Kotamraju
 */
public class JavaSeContainer extends AbstractApplicationContainer {

    //private final int port;
    private final Set<String> unsupportedUses;

    public JavaSeContainer(WsTool wsimport, WsTool wsgen, int port, Set<String> unsupportedUses) {
        super(wsimport, wsgen);
        //this.port = port;
        this.unsupportedUses = unsupportedUses;
    }

    @Override
    @NotNull
    public Set<String> getUnsupportedUses() {
        return unsupportedUses;
    }

    public String getTransport() {
        return "http";
    }

    public void start() throws Exception {
    }

    public void shutdown() {
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        final String id = service.service.getGlobalUniqueName();
        final WAR war = assembleWar(service);
        List<EndpointInfoBean> beans = war.getEndpointInfoBeans();

        // Using a free port rather than a standard port since starting and
        // stopping multiple times has BindingException with light weight http server
        URL baseAddress = new URL("http://localhost:" + getFreePort() + "/" + id+"/");

        // serviceClassLoader.getResource("WEB-INF/wsdl/xxx.wsdl") should work,
        // so adjust the classpath accordingly
        final URLClassLoader serviceClassLoader = new URLClassLoader(new URL[]{service.warDir.toURL(), new File(service.warDir, "WEB-INF/classes").toURL()},
                                                                     World.runtime.getClassLoader());


        Object[] servers = new Object[service.service.endpoints.size()];
        int i = 0;
        for(TestEndpoint testEndpoint :service.service.endpoints) {

            final InterpreterEx interpreter = new InterpreterEx(serviceClassLoader);

            // Associate an EndpointInfoBean with the TestEndpoint
            EndpointInfoBean endpointInfoBean = null;
            for(Object bean : beans.toArray()) {
                EndpointInfoBean ebean = (EndpointInfoBean)bean;
                if (ebean.getImplementation().equals(testEndpoint.className)) {
                    endpointInfoBean = ebean;
                    break;
                }
            }

            final Class endpointClass = serviceClassLoader.loadClass(testEndpoint.className);

            final Object endpointImpl = endpointClass.newInstance();

            // Check if primary wsdl is specified via @WebService(wsdlLocation="")
            String wsdlLocation = null;
            Annotation[] anns = endpointClass.getAnnotations();
            for(Annotation ann : anns) {
                try {
                    Method method = ann.getClass().getDeclaredMethod("wsdlLocation");
                    String str = (String)method.invoke(ann);
                    if (!str.equals("")) {
                        wsdlLocation = str;
                        break;
                    }
                } catch(NoSuchMethodException e) {
                    // OK, the annotation does not support wsdlLocation() method
                }
            }

            // Collect all WSDL, Schema metadata for this service
            final List<Source> metadata = new ArrayList<Source>();
            collectDocs(service.warDir.getCanonicalPath()+"/", "WEB-INF/wsdl/", serviceClassLoader, metadata);
            
            // primary wsdl shouldn't be added, if it is already set via @WebService(wsdlLocation=)
            if (wsdlLocation != null) {
                Iterator<Source> it = metadata.iterator();
                while(it.hasNext()) {
                    Source source = it.next();
                    if (source.getSystemId().endsWith(wsdlLocation)) {
                        it.remove();
                    }
                }
            }
            System.out.print("Setting metadata="+metadata);

            // Set service name, port name
            Map<String, Object> props = new HashMap<String, Object>();
            if (endpointInfoBean.getServiceName() != null) {
                // Endpoint.WSDL_SERVICE
                props.put("javax.xml.ws.wsdl.service", endpointInfoBean.getServiceName());
            }
            if (endpointInfoBean.getPortName() != null) {
                // Endpoint.WSDL_PORT
                props.put("javax.xml.ws.wsdl.port", endpointInfoBean.getPortName());
            }
            System.out.println("Setting properties="+props);

            String endpointAddress = baseAddress+ testEndpoint.name;
            System.out.println("Endpoint Address="+endpointAddress);

            interpreter.set("endpointAddress", endpointAddress);
            interpreter.set("endpointImpl", endpointImpl);
            interpreter.set("metadata", metadata);
            interpreter.set("properties", props);

            // TODO handlers
            servers[i++] = interpreter.eval(
                    "endpoint = javax.xml.ws.Endpoint.create(endpointImpl);" +
                    "endpoint.setMetadata(metadata);" +
                    "endpoint.setProperties(properties);" +
                    "endpoint.publish(endpointAddress);" +
                    "return endpoint;");
        }
        return new JavaSeApplication(servers, baseAddress, service);
    }

    private Set<String> getResourcePaths(String root, String path) {
        Set<String> r = new HashSet<String>();
        File[] files = new File(root+path).listFiles();
        if (files == null) {
            return null;
        }
        for( File f : files) {
            if(f.isDirectory()) {
                r.add(path+f.getName()+'/');
            } else {
                r.add(path+f.getName());
            }
        }
        return r;
    }

    private static int getFreePort() {
        int port = -1;
        try {
            ServerSocket soc = new ServerSocket(0);
            port = soc.getLocalPort();
            soc.close();
        } catch (IOException e) {
        }
        return port;
    }


    /*
     * Get all the WSDL & schema documents recursively.
     */
    private void collectDocs(String root, String dirPath, ClassLoader loader, List<Source> metadata) throws IOException {
        Set<String> paths = getResourcePaths(root, dirPath);
        if (paths != null) {
            for (String path : paths) {
                if (path.endsWith("/")) {
                    collectDocs(root, path, loader, metadata);
                } else {
                    URL res = loader.getResource(path);
                    metadata.add(new StreamSource(res.openStream(), res.toExternalForm()));
                }
            }
        }
    }

}