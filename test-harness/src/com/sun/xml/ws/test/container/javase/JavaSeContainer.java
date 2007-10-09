/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006, 2007 Sun Microsystems Inc. All Rights Reserved
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
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
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

    final private String sepChar;
    final private File webappsDir;
    final private File classesDir;
    private boolean stopped;
    final private int port;
    
    public JavaSeContainer(WsTool wsimport, WsTool wsgen, int port) {
        super(wsimport,wsgen);
        String j2seServerDir = System.getProperty ("j2se.server.home");  // TODO -- Get location from main
        System.out.println("Server dir="+j2seServerDir);
        sepChar = System.getProperty ("file.separator");
        webappsDir = new File(j2seServerDir+sepChar+"webapps");
        classesDir = new File(webappsDir, "classes");
        System.out.println("webapps dir="+webappsDir.getAbsolutePath());
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

        if (service.service.isSTS)
            updateWsitClient(service, endpointAddress);

        HashMap<QName,String> portToNamespace = new HashMap<QName,String>();
        for (File wsdl : war.getWSDL()) {
            patchWsdl(wsdl,endpointAddress);
        }

        final URLClassLoader serviceClassLoader = new URLClassLoader(
            new URL[]{new File(service.warDir,"WEB-INF/classes").toURL()},
            World.runtime.getClassLoader());
        final InterpreterEx interpreter = new InterpreterEx(serviceClassLoader);
        final TestEndpoint testEndpoint = (TestEndpoint) service.service.endpoints.toArray()[0];
        final Class endpointClass = serviceClassLoader.loadClass(testEndpoint.className);
        
        final Object endpointImpl = endpointClass.newInstance();
        interpreter.set("endpointAddress",endpointAddress);
        interpreter.set("endpointImpl",endpointImpl);
        interpreter.set("metadataFiles", war.getWSDL());
        
        final Object server = interpreter.eval(
            "java.util.List metadata = new java.util.LinkedList();" +
            "for(java.io.File file : metadataFiles) {" +
            "    javax.xml.transform.Source source = new javax.xml.transform.stream.StreamSource(new java.io.FileInputStream(file));" +
            "    source.setSystemId(file.toURL().toExternalForm());" +
            "    metadata.add(source);" +
            "}" +
            "javax.xml.ws.Endpoint endpoint = javax.xml.ws.Endpoint.create(endpointImpl);" +
            "System.out.println(\"endpointAddress = \" + endpointAddress);" +
            "endpoint.setMetadata(metadata);" +
            "endpoint.publish(endpointAddress);" +
            "return endpoint;");

        return new JavaSeApplication(war,server,new URI(endpointAddress));
    }

    
    public void updateWsitClient(DeployedService deployedService, String newLocation)
        throws DocumentException, IOException {
        File wsitClientFile = new File(deployedService.service.parent.resources,"wsit-client.xml");
        if (wsitClientFile.exists() ){
            SAXReader reader = new SAXReader();
            Document document = reader.read(wsitClientFile);
            Element root = document.getRootElement();
            Element policy = root.element("Policy");
            Element sts = policy.element("ExactlyOne").element("All").element("PreconfiguredSTS");

            Attribute  endpoint = sts.attribute("endpoint");
            
            newLocation = newLocation.replace('\\', '/');
            endpoint.setValue(newLocation);

            Attribute wsdlLoc = sts.attribute("wsdlLocation");
            wsdlLoc.setValue(deployedService.service.wsdl.wsdlFile.toURI().toString());

            XMLWriter writer = new XMLWriter(new FileWriter(wsitClientFile));
            writer.write( document );
            writer.close();

        } else {
            throw new RuntimeException("wsit-client.xml is absent. It is required. \n"+
                    "Please check " + deployedService.service.parent.resources );
        }
    }

    
    /**
     * Fix the address in the WSDL. to the local address.
     */
    private void patchWsdl(File wsdl, String endpointAddress) throws Exception {
        Document doc = new SAXReader().read(wsdl);
        List ports = doc.getRootElement().element("service").elements("port");

        for (Object port : ports) {
            Element castPort = (Element) port;
            String portName = castPort.attributeValue("name");
            Element address = getSoapAddress(castPort);

            //Looks like invalid wsdl:port, MUST have a soap:address
            if(address == null)
                throw new RuntimeException("Did not find a soap:address for wsdl:port " + portName);

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

    private Element getSoapAddress(Element port){
        for(Object obj : port.elements()){
            Element address = (Element) obj;

            //it might be extensibility element, just skip it
            if(!address.getName().equals("address"))
                continue;

            if(address.getNamespaceURI().equals("http://schemas.xmlsoap.org/wsdl/soap/") ||
               address.getNamespaceURI().equals("http://schemas.xmlsoap.org/wsdl/soap12/"))
            return address;
        }
        return null;
    }

}