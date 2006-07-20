package com.sun.xml.ws.test.container.gf;

import com.sun.xml.ws.test.container.AbstractHttpApplication;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;

import javax.enterprise.deploy.spi.TargetModuleID;
import java.net.URL;

/**
 * {@link Application} implementation for {@link GlassfishContainer}.
 *
 * @author Kohsuke Kawaguchi
 */
final class GlassfishApplication extends AbstractHttpApplication {

    private final GlassfishContainer container;

    /**
     * These JSR-88 objects represent the deployed modules.
     */
    private final TargetModuleID[] modules;

    public GlassfishApplication(URL warURL, DeployedService service, GlassfishContainer container, TargetModuleID[] modules) {
        super(warURL, service);
        this.container = container;
        this.modules = modules;
    }

    public void undeploy() throws Exception {
        container.undeploy(modules);
    }
}
