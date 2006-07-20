package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.net.URI;
import java.net.URL;

/**
 * Partial {@link Application} implementation for web containers.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractHttpApplication implements Application {
    /**
     * URL to access this web application.
     */
    protected final URL warURL;

    protected final DeployedService service;

    protected AbstractHttpApplication(URL warURL, DeployedService service) {
        this.warURL = warURL;
        this.service = service;
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
        // somehow relative path computation doesn't work, so I rely on String concatanation. Ouch!
        return new URL(getEndpointAddress(endpoint).toString()+"?wsdl");
    }

}
