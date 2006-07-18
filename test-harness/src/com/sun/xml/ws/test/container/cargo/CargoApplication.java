package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.istack.NotNull;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployer.Deployer;

import java.net.URI;
import java.net.URL;

/**
 * {@link Application} implementation for Cargo.
 *
 * @author Kohsuke Kawaguchi
 */
final class CargoApplication implements Application {
    private final Deployer deployer;
    private final Deployable war;

    public CargoApplication(Deployer deployer, Deployable war) {
        this.deployer = deployer;
        this.war = war;
    }

    public void undeploy() throws Exception {
        System.out.println("Undeploying a service");
        deployer.undeploy(war);
    }

    @NotNull
    public URI getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * When deployed to HTTP service, WSDL URL can be obtained by "?wsdl".
     */
    @NotNull
    public URL getWSDL() throws Exception {
        // TODO: there's wrong "one-service/one-endpoint" assumption here
        return new URL(getEndpointAddress(null/*TODO*/).toURL(),"?wsdl");
    }
}
