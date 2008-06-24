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

package com.sun.xml.ws.test.exec;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.TargetError;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.client.ScriptBaseClass;
import com.sun.xml.ws.test.client.XmlResource;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.model.TestClient;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.World;

import java.beans.Introspector;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map.Entry;

/**
 * Executes {@link TestClient}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClientExecutor extends Executor {
    /**
     * Client test scenario to execute.
     */
    private final TestClient client;

    public ClientExecutor(DeploymentContext context, TestClient client) {
        super("client "+client.script.getName().replace('.','_'), context);
        this.client = client;
    }

    public void runBare() throws Throwable {
        if(context.clientClassLoader==null) {
            context.clientClassLoader = context.descriptor.resources != null
                    ? new URLClassLoader(new URL[]{context.descriptor.resources.toURL()}, World.runtime.getClassLoader())
                    : World.runtime.getClassLoader();
        }

        Interpreter engine = new InterpreterEx(context.clientClassLoader);

        NameSpace ns = engine.getNameSpace();
        // import namespaces. what are the other namespaces to be imported?
        ns.importPackage("javax.activation");
        ns.importPackage("javax.xml.ws");
        ns.importPackage("javax.xml.ws.soap");
        ns.importPackage("javax.xml.ws.handler");
        ns.importPackage("javax.xml.ws.handler.soap");
        ns.importPackage("javax.xml.bind");
        ns.importPackage("javax.xml.soap");
        ns.importPackage("javax.xml.namespace");
        ns.importPackage("javax.xml.transform");
        ns.importPackage("javax.xml.transform.sax");
        ns.importPackage("javax.xml.transform.dom");
        ns.importPackage("javax.xml.transform.stream");
        ns.importPackage("java.util");
        ns.importPackage("java.util.concurrent");
        // if there's any client package, put them there
        ns.importPackage(context.descriptor.name+".client");
        ns.importPackage(context.descriptor.name+".common");

        // this will make 'thisObject' available as 'this' in script
        ns.importObject(new ScriptBaseClass(context, engine, client));

        // load additional helper methods
        try {
            engine.eval(new InputStreamReader(getClass().getResourceAsStream("util.bsh")));
        } catch (EvalError evalError) {
            throw new Error("Failed to evaluate util.bsh",evalError);
        }

        // when invoking JAX-WS, we need to set the context classloader accordingly
        // so that it can discover classes from the right places.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(context.clientClassLoader);

        try {
            injectResources(ns, engine);
            invoke(engine);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Makes the test script invocation.
     */
    protected void invoke(Interpreter engine) throws Throwable {
        // executes the script
        Reader r = client.script.read();
        try {
            if(client.parent.setUpScript!=null)
                engine.eval(new StringReader(client.parent.setUpScript),
                    engine.getNameSpace(), "pre-client script" );
            engine.eval(r, engine.getNameSpace(), client.script.getName() );
        } catch(TargetError e) {
            throw e.getTarget();
        } finally {
            r.close();
        }
    }

    private void injectResources(NameSpace ns, Interpreter engine) throws Exception {
        StringBuilder serviceList = new StringBuilder("injected services:");
        StringBuilder portList = new StringBuilder("injected ports:");
        StringBuilder addressList = new StringBuilder("injected addresses:");

        // inject test home directory
        engine.set("home",client.parent.home);

        // inject XML resources
        for (Entry<String, XmlResource> e : context.descriptor.xmlResources.entrySet())
            engine.set(e.getKey(),e.getValue());

        for (DeployedService svc : context.services.values()) {
            if (! svc.service.isSTS) {
                // inject WSDL URLs
                engine.set("wsdlUrls",svc.app.getWSDL());

                /*
                 TODO: some more work here
                // Server side may be provider endpoint that doesn't expose WSDL
                // So there are no generated services.
                if (svc.serviceClass.size() == 0) {

                    String portName = null;
                    for (TestEndpoint e : svc.service.endpoints) {
                        portName = e.portName;
                        break;
                    }
                    String varName = Introspector.decapitalize(portName);
                    engine.set(varName +"Address",svc.app.getEndpointAddress(getEndpoint(svc, portName)));
                    addressList.append(' ').append(varName).append("Address");
                    continue;
                }
                */

                for (Class clazz : svc.serviceClass) {
                    String packageName = clazz.getPackage().getName();
                    //  import the artifact package
                    ns.importPackage(packageName);
                    //  use reflection to list up all methods with 'javax.xml.ws.WebEndpoint' annotations
                    //  invoke that method via reflection to obtain the Port object.
                    //  set the endpoint address to that port object
                    //  inject it to the scripting engine
                    Method[] methods = clazz.getMethods();

                    // annotation that serviceClass loads and annotation that this code
                    // uses might be different
                    Class<? extends Annotation> webendpointAnnotation = clazz.getClassLoader()
                            .loadClass("javax.xml.ws.WebEndpoint").asSubclass(Annotation.class);
                    Method nameMethod = webendpointAnnotation.getDeclaredMethod("name");

                    Object serviceInstance = clazz.newInstance();

                    {// inject a service instance
                        String serviceVarName = Introspector.decapitalize(clazz.getSimpleName());
                        engine.set(serviceVarName,serviceInstance);
                        serviceList.append(' ').append(serviceVarName);
                    }

                    for (Method method : methods) {
                        Annotation endpoint = method.getAnnotation(webendpointAnnotation);
						// don't inject variables for methods like getHelloPort(WebServiceFeatures)
                        if (endpoint != null && method.getParameterTypes().length == 0) {

                            //For multiple endpoints the convention for injecting the variables is
                            // portName obtained from the WebEndpoint annotation,
                            // which would be something like "addNumbersPort"
                            String portName = (String) nameMethod.invoke(endpoint);
                            String varName = Introspector.decapitalize(portName);

                            try {
                                engine.set(varName, method.invoke(serviceInstance));
                                engine.set(varName +"Address",svc.app.getEndpointAddress(getEndpoint(svc, portName)));
                                addressList.append(' ').append(varName).append("Address");
                            } catch (InvocationTargetException e) {
                                if(e.getCause() instanceof Exception)
                                    throw (Exception)e.getCause();
                                else
                                    throw e;
                            }
                            portList.append(' ').append(varName);
                        }
                    }
                }
            }
        }
        System.out.println(serviceList);
        System.out.println(portList);
        System.out.println(addressList);
    }

    private TestEndpoint getEndpoint(DeployedService svc, String portName) {
        // first, look for the port name match
        for (TestEndpoint e : svc.service.endpoints) {
            if(e.portName!=null && e.portName.equals(portName))
                return e;
        }
        // nothing explicitly matched.
        if(svc.service.endpoints.size()!=1)
            throw new Error("Multiple ports are defined on '"+svc.service.name+"', yet ports are ambiguous. Please use @WebService/Provider(portName=)");
        // there's only one
        return (TestEndpoint)svc.service.endpoints.toArray()[0];
    }
}
