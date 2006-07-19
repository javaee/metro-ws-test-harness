/*
 * LocalApplication.java
 *
 * Created on June 28, 2006, 10:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.xml.ws.test.container.local;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.net.URI;
import java.net.URL;

/**
 * {@link Application} implementation for {@link LocalApplicationContainer}.
 *
 * @author ken
 */
final class LocalApplication implements Application {

    private final @NotNull WAR war;

    private final @NotNull URI endpointAddress;

    /** Creates a new instance of LocalApplication */
    LocalApplication(@NotNull WAR war, URI endpointAddress) {
        this.war = war;
        this.endpointAddress = endpointAddress;
    }

    /**
     * Returns the actual endpoint address to which the given {@link TestEndpoint}
     * is deployed.
     */
    @NotNull
    public URI getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception {
        return endpointAddress;
    }

    /**
     * Gets the WSDL of this service.
     *
     * <p>
     * This WSDL will be compiled to generate client artifacts during a test.
     */
    @NotNull
    public URL getWSDL() throws Exception {
        return war.getWSDL().toURL();
    }

    /**
     * Removes this application from the container.
     */
    public void undeploy() throws Exception {
        // no-op. don't clean up artifacts since those are often necessary
        // to diagnose problems when the user is debugging a problem.

        // instead, clean up is done in LocalApplicationContainer.deploy()
    }
}
