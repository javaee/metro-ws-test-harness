package com.sun.xml.ws.test.container.local;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.tool.WsTool;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.List;

/**
 * {@link ApplicationContainer} for the local transport.
 *
 * @author Kohsuke Kawaguchi
 */
public class LocalApplicationContainer extends AbstractApplicationContainer {

    public LocalApplicationContainer(WsTool wsimport, WsTool wsgen, boolean debug) {
        super(wsimport,wsgen,debug);
    }

    public void start() {
        // noop
    }

    public void shutdown() {
        // noop
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        WAR war = assembleWar(service);
        URI endpointAddress = patchWsdl(service,war.getWSDL());
        return new LocalApplication(war,endpointAddress);
    }

    /**
     * Fix the address in the WSDL. to the local address.
     *
     * This doesn't take into account multiple endpoints, url patterns, etc
     *
     * @return
     *      the local endpoint URI that points to this address.
     */
    private URI patchWsdl(DeployedService service, File wsdl) throws Exception {
        Document doc = new SAXReader().read(wsdl);
        List<Element> ports = doc.getRootElement().element("service").elements("port");

        // TODO: this code incorrectly assumes one service = one endpoint
        String newLocation = null;

        for (Element port : ports) {
            String portName = port.attributeValue("name");

            Element address = (Element)port.elements().get(0);

            Attribute locationAttr = address.attribute("location");
            newLocation =
                "local://" + service.warDir.getAbsolutePath() + "?" + portName;
            newLocation = newLocation.replace('\\', '/');
            locationAttr.setValue(newLocation);
        }

        // save file
        FileOutputStream os = new FileOutputStream(wsdl);
        new XMLWriter(os).write(doc);
        os.close();

        return new URI(newLocation);
    }

}
