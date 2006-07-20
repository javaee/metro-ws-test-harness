package com.sun.xml.ws.test.container;

import com.sun.xml.ws.test.container.jelly.EndpointInfoBean;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.World;

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

    /**
     * Prepares a fully packaged war file to the specified location.
     */
    protected final WAR createWARZip(DeployedService service, File archive) throws Exception {
        WAR assembly = assembleWar(service);

        // copy runtime classes into the classpath. this is slow.
        // isn't there a better way to do this?
        if(copyRuntimeLibraries()) {
            System.out.println("Copying runtime libraries");
            assembly.copyClasspath(World.runtime);
        }

        System.out.println("Assembling a war file");
        assembly.zipTo(archive);

        return assembly;
    }

    /**
     * Copy JAX-WS runtime code?
     */
    protected boolean copyRuntimeLibraries() {
        return true;
    }
}
