package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

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
    public List<URL> getWSDL() throws Exception {
        List<URL> urls = new ArrayList<URL>();

        // TODO: if those endpoints point belong to the same service,
        // we end up returning multiple WSDLs that are really the same.
        // this should be harmless in terms of correctness, but
        // it's inefficient, as we'll do extra compilation.
        // can we avoid that?
        for (TestEndpoint ep : service.service.endpoints) {
            // somehow relative path computation doesn't work, so I rely on String concatanation. Ouch!
            urls.add(new URL(getEndpointAddress(ep)+"?wsdl"));
        }
        return urls;
    }

}
