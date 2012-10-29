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

package com.sun.xml.ws.test.container.invm;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.client.InterpreterEx;
import java.io.FileWriter;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.List;

/**
 * {@link ApplicationContainer} for the local transport.
 *
 * @author Kohsuke Kawaguchi
 */
public class InVmContainer extends AbstractApplicationContainer {

    public InVmContainer(WsTool wsimport, WsTool wsgen) {
        super(wsimport,wsgen,false);
    }

    public String getTransport() {
        return "in-vm";
    }

    public void start() {
        // noop
    }

    public void shutdown() {
        // noop
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        String id = service.service.getGlobalUniqueName();
        WAR war = assembleWar(service);

        if (service.service.isSTS)
            updateWsitClient(service,id);

        for (File wsdl : war.getWSDL())
            patchWsdl(service,wsdl,id);

        URLClassLoader serviceClassLoader = new URLClassLoader(
            new URL[]{new File(service.warDir,"WEB-INF/classes").toURL()},
            World.runtime.getClassLoader());
        InterpreterEx i = new InterpreterEx(serviceClassLoader);
        i.set("id",id);
        i.set("dir",service.warDir);
        Object server = i.eval("new com.sun.xml.ws.transport.local.InVmServer(id,dir)");

        return new InVmApplication(war,server,new URI("in-vm://"+id+"/"));
    }

    public void updateWsitClient(DeployedService deployedService, String id)throws Exception {
        File wsitClientFile = new File(deployedService.service.parent.resources,"wsit-client.xml");
        if (wsitClientFile.exists() ){
            SAXReader reader = new SAXReader();
            Document document = reader.read(wsitClientFile);
            Element root = document.getRootElement();
            Element policy = root.element("Policy");
            Element sts = policy.element("ExactlyOne").element("All").element("PreconfiguredSTS");

            Attribute  endpoint = sts.attribute("endpoint");
            TestEndpoint foo = (TestEndpoint) deployedService.service.endpoints.toArray()[0];
            String newLocation =
                "in-vm://" + id + "/";
            newLocation = newLocation.replace('\\', '/');
            endpoint.setValue(newLocation);

            Attribute wsdlLoc = sts.attribute("wsdlLocation");
            wsdlLoc.setValue(deployedService.service.wsdl.get(0).wsdlFile.toURI().toString());

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
    private void patchWsdl(DeployedService service, File wsdl, String id) throws Exception {
        Document doc = new SAXReader().read(wsdl);
        List<Element> ports = doc.getRootElement().element("service").elements("port");

        for (Element port : ports) {
            String portName = port.attributeValue("name");
            Element address = getSoapAddress(port);

            //Looks like invalid wsdl:port, MUST have a soap:address
            //TODO: give some error message
            if (address == null)
                continue;
            if (!"wsdl".equalsIgnoreCase(wsdl.getParentFile().getName())) {
                portName = wsdl.getParentFile().getName() + portName;
            }
            Attribute locationAttr = address.attribute("location");
            String newLocation =
                    "in-vm://" + id + "/?" + portName;
            newLocation = newLocation.replace('\\', '/');
            locationAttr.setValue(newLocation);

            //Patch wsa:Address in wsa:EndpointReference as well
            Element wsaEprEl = port.element(QName.get("EndpointReference", "wsa", "http://www.w3.org/2005/08/addressing"));
            if (wsaEprEl != null) {
                Element wsaAddrEl = wsaEprEl.element(QName.get("Address", "wsa", "http://www.w3.org/2005/08/addressing"));
                wsaAddrEl.setText(newLocation);

            }
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
