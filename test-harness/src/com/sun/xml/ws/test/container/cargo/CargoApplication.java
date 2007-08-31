package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.container.AbstractHttpApplication;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployer.Deployer;

import java.net.URL;

/**
 * {@link Application} implementation for Cargo.
 *
 * @author Kohsuke Kawaguchi
 */
final class CargoApplication extends AbstractHttpApplication {
    private final Deployer deployer;
    private final Deployable war;

    public CargoApplication(Deployer deployer, Deployable war, URL warURL, DeployedService service) {
        super(warURL,service);
        this.deployer = deployer;
        this.war = war;
    }

    public void undeploy() throws Exception {
        System.out.println("Undeploying a service");
        deployer.undeploy(war);
    }
}
