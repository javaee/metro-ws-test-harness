package com.sun.xml.ws.test.container.cargo;

import org.codehaus.cargo.container.internal.RunnableContainer;
import org.codehaus.cargo.container.LocalContainer;
import com.sun.xml.ws.test.tool.WsTool;

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

    protected AbstractRunnableCargoContainer(WsTool wsimport, WsTool wsgen) {
        super(wsimport, wsgen);

        // set TCP port to somewhere between 20000-30000
        httpPort = new Random().nextInt(10000) + 20000;
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
