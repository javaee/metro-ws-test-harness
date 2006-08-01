package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.LocalContainer;

import java.net.URL;
import java.util.Random;

/**
 * Common implementation of {@link EmbeddedCargoApplicationContainer}
 * and {@link InstalledCargoApplicationContainer}.
 *
 * This class also assumes that the launched container can be accessible
 * by "localhost".
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractRunnableCargoContainer<C extends LocalContainer> extends AbstractCargoContainer<C> {

    protected final int httpPort;

    protected AbstractRunnableCargoContainer(WsTool wsimport, WsTool wsgen, int port) {
        super(wsimport, wsgen);
        httpPort = port;
    }

    public void start() throws Exception {
        System.out.println("Starting "+container.getId());
        container.start();
    }

    public void shutdown() throws Exception {
        System.out.println("Stopping "+container.getId());
        container.stop();
    }

    protected URL getServiceUrl(String contextPath) throws Exception {
        return new URL("http", "localhost", httpPort, "/" + contextPath + "/");
    }
}
