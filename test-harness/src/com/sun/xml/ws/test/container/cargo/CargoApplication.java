package com.sun.xml.ws.test.container.cargo;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.model.TestEndpoint;
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

    /**
     * URL to access this web application.
     */
    private final URL warURL;

    private final DeployedService service;

    public CargoApplication(Deployer deployer, Deployable war, URL warURL, DeployedService service) {
        this.deployer = deployer;
        this.war = war;
        this.warURL = warURL;
        this.service = service;
    }

    public void undeploy() throws Exception {
        System.out.println("Undeploying a service");
        deployer.undeploy(war);
    }

    @NotNull
    public URI getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception {
        return new URL(warURL,endpoint.name).toURI();
    }

    /**
     * When deployed to HTTP service, WSDL URL can be obtained by "?wsdl".
     */
    @NotNull
    public URL getWSDL() throws Exception {
        // assume all the endpoints have the same WSDL.
        // this is guaranteed for fromwsdl. For fromjava, this is also
        // guaranteed by the fact that we only support one endpoint per service.
        TestEndpoint endpoint = service.service.endpoints.iterator().next();
        return new URL(getEndpointAddress(endpoint).toURL(),"?wsdl");
    }
}
