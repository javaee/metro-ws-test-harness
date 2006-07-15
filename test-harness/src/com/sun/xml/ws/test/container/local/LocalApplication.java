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
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.net.URL;
import java.io.File;

/**
 * {@link Application} implementation for {@link LocalApplicationContainer}.
 *
 * @author ken
 */
final class LocalApplication implements Application {

    /**
     * Generated or provided WSDL.
     */
    private final File wsdl;

    /** Creates a new instance of LocalApplication */
    LocalApplication(@NotNull DeployedService service, @NotNull File wsdl) {
        this.service = service;
        this.wsdl = wsdl;
    }

    private final DeployedService service;

    /**
     * Returns the actual endpoint address to which the given {@link TestEndpoint}
     * is deployed.
     */
    @NotNull
    public String getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception {
        return endpoint.name;
    }

    /**
     * Gets the WSDL of this service.
     *
     * <p>
     * This WSDL will be compiled to generate client artifacts during a test.
     */
    @NotNull
    public URL getWSDL() throws Exception {
        return wsdl.toURL();
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
