package com.sun.xml.ws.test.container.invm;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.client.InterpreterEx;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
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
        super(wsimport,wsgen);
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

    /**
     * Fix the address in the WSDL. to the local address.
     */
    private void patchWsdl(DeployedService service, File wsdl, String id) throws Exception {
        Document doc = new SAXReader().read(wsdl);
        List<Element> ports = doc.getRootElement().element("service").elements("port");

        for (Element port : ports) {
            String portName = port.attributeValue("name");

            Element address = (Element)port.elements().get(0);

            Attribute locationAttr = address.attribute("location");
            String newLocation =
                "in-vm://" + id + "/?" + portName;
            newLocation = newLocation.replace('\\', '/');
            locationAttr.setValue(newLocation);
        }

        // save file
        FileOutputStream os = new FileOutputStream(wsdl);
        new XMLWriter(os).write(doc);
        os.close();
    }

}
