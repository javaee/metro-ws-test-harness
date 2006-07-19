package com.sun.xml.ws.test.container;

import com.sun.xml.ws.test.container.jelly.EndpointInfoBean;
import com.sun.xml.ws.test.tool.WsTool;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 * Base implementation of {@link ApplicationContainer}.
 *
 * This implementation provides code for common tasks, such as assembling files
 * into a war, etc.
 *
 * @author Kohsuke Kawaguchi
 * @author Ken Hofsass
 */
public abstract class AbstractApplicationContainer implements ApplicationContainer {
    private final WsTool wsimport;
    private final WsTool wsgen;

    protected AbstractApplicationContainer(WsTool wsimport, WsTool wsgen) {
        this.wsimport = wsimport;
        this.wsgen = wsgen;
    }

    /**
     * Prepares an exploded war file image for this service.
     */
    protected final WAR assembleWar(DeployedService service) throws Exception {
        WAR war = new WAR(service);

        boolean fromJava = (service.service.wsdl==null);

        if(!fromJava)
            war.compileWSDL(wsimport);
        war.compileJavac();
        if(fromJava)
            war.generateWSDL(wsgen);

        List<EndpointInfoBean> endpoints = war.generateSunJaxWsXml();
        war.generateWebXml(endpoints);

        PrintWriter w = new PrintWriter(new FileWriter(new File(war.root, "index.html")));
        w.println("<html><body>Deployed by the JAX-WS test harness</body></html>");
        w.close();

        return war;
    }
}
