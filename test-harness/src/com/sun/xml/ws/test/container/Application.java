package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.net.URL;

/**
 * Represents an application deployed inside a {@link ApplicationContainer}.
 *
 * <p>
 * This object needs to be multi-thread safe.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Application {
    /**
     * Returns the actual endpoint address to which the given {@link TestEndpoint}
     * is deployed.
     */
    @NotNull String getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception;

    /**
     * Gets the WSDL of this service.
     *
     * <p>
     * This WSDL will be compiled to generate client artifacts during a test.
     */
    @NotNull URL getWSDL() throws Exception;

    /**
     * Removes this application from the container.
     */
    void undeploy() throws Exception;
}
