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
import com.sun.net.httpserver.HttpServer;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.client.InterpreterEx;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class JavaSeContainer extends AbstractApplicationContainer {

    private HttpServer appServer;
    private ExecutorService appExecutorService;
    private final String sepChar;
    private final File webappsDir;
    private final File classesDir;
    private boolean stopped;
    private final int port;

    public JavaSeContainer(WsTool wsimport, WsTool wsgen, int port) {
        super(wsimport, wsgen);
        String j2seServerDir = System.getProperty("j2se.server.home"); // TODO -- Get location from main
        System.out.println("Server dir=" + j2seServerDir);
        sepChar = System.getProperty("file.separator");
        webappsDir = new File(j2seServerDir + sepChar + "webapps");
        classesDir = new File(webappsDir, "classes");
        System.out.println("webapps dir=" + webappsDir.getAbsolutePath());
        this.port = port;
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

        final String endpointAddress = new String("http://localhost:" + port + "/" + id);

        if (service.service.isSTS) {
            updateWsitClient(service, endpointAddress);
        }
        HashMap<QName, String> portToNamespace = new HashMap<QName, String>();
        for (File wsdl : war.getWSDL()) {
            patchWsdl(wsdl, endpointAddress);
        }

        final URLClassLoader serviceClassLoader = new URLClassLoader(new URL[]{new File(service.warDir, "WEB-INF/classes").toURL()},
                                                                     World.runtime.getClassLoader());
        final InterpreterEx interpreter = new InterpreterEx(serviceClassLoader);
        final TestEndpoint testEndpoint = (TestEndpoint) service.service.endpoints.toArray()[0];
        final Class endpointClass = serviceClassLoader.loadClass(testEndpoint.className);

        final Object endpointImpl = endpointClass.newInstance();

        final List<Source> metadata = new LinkedList<Source>();
        for (File file : war.getWSDL()) {
            Source source = new StreamSource(new FileInputStream(file));
            source.setSystemId(file.toURL().toExternalForm());
            metadata.add(source);
        }
        
        interpreter.set("endpointAddress", endpointAddress);
        interpreter.set("endpointImpl", endpointImpl);
        interpreter.set("metadata", metadata);

        final Object server = interpreter.eval(
                "javax.xml.ws.Endpoint endpoint = javax.xml.ws.Endpoint.create(endpointImpl);" +
                "System.out.println(\"endpointAddress = \" + endpointAddress);" +
                "endpoint.setMetadata(metadata);" +
                "endpoint.publish(endpointAddress);" +
                "return endpoint;");

        return new JavaSeApplication(war, server, new URI(endpointAddress));
    }

    public void updateWsitClient(DeployedService deployedService, String newLocation) throws DocumentException, IOException {
        File wsitClientFile = new File(deployedService.service.parent.resources, "wsit-client.xml");
        if (wsitClientFile.exists()) {
            SAXReader reader = new SAXReader();
            Document document = reader.read(wsitClientFile);
            Element root = document.getRootElement();
            Element policy = root.element("Policy");
            Element sts = policy.element("ExactlyOne").element("All").element("PreconfiguredSTS");

            Attribute endpoint = sts.attribute("endpoint");

            newLocation = newLocation.replace('\\', '/');
            endpoint.setValue(newLocation);

            Attribute wsdlLoc = sts.attribute("wsdlLocation");
            wsdlLoc.setValue(deployedService.service.wsdl.wsdlFile.toURI().toString());

            XMLWriter writer = new XMLWriter(new FileWriter(wsitClientFile));
            writer.write(document);
            writer.close();
        } else {
            throw new RuntimeException("wsit-client.xml is absent. It is required. \n" +
                                       "Please check " + deployedService.service.parent.resources);
        }
    }

    /**
     * Fix the address in the WSDL. to the local address.
     */
    private void patchWsdl(File wsdl, String endpointAddress) throws Exception {
        Document doc = new SAXReader().read(wsdl);
        List ports = doc.getRootElement().element("service").elements("port");

        for (Object wsdlPort : ports) {
            Element castPort = (Element) wsdlPort;
            String portName = castPort.attributeValue("name");
            Element address = getSoapAddress(castPort);

            //Looks like invalid wsdl:port, MUST have a soap:address
            if (address == null) {
                throw new RuntimeException("Did not find a soap:address for wsdl:port " + portName);
            }
            Attribute locationAttr = address.attribute("location");
            String newLocation = endpointAddress + "/" + portName;
            newLocation = newLocation.replace('\\', '/');
            locationAttr.setValue(newLocation);
        }

        // save file
        FileOutputStream os = new FileOutputStream(wsdl);
        new XMLWriter(os).write(doc);
        os.close();
    }

    private Element getSoapAddress(Element port) {
        for (Object obj : port.elements()) {
            Element address = (Element) obj;

            //it might be extensibility element, just skip it
            if (!address.getName().equals("address")) {
                continue;
            }
            if (address.getNamespaceURI().equals("http://schemas.xmlsoap.org/wsdl/soap/") || address.getNamespaceURI().equals("http://schemas.xmlsoap.org/wsdl/soap12/")) {
                return address;
            }
        }
        return null;
    }
}