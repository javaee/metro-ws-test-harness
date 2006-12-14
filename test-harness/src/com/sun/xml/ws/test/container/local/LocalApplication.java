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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Application} implementation for {@link LocalApplicationContainer}.
 *
 * @author ken
 * @deprecated
 *      To be removed once in-vm transport becomes ready
 */
final class LocalApplication implements Application {

    private final @NotNull WAR war;

    /**
     * "local://path/to/exploded/dir" portion of the endpoint address.
     * Adding "?portName" makes it the full endpoint address.
     */
    private final @NotNull URI baseEndpointAddress;

    /** Creates a new instance of LocalApplication */
    LocalApplication(@NotNull WAR war, URI endpointAddress) {
        this.war = war;
        this.baseEndpointAddress = endpointAddress;
    }

    /**
     * Returns the actual endpoint address to which the given {@link TestEndpoint}
     * is deployed.
     */
    @NotNull
    public URI getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception {
        return new URI(baseEndpointAddress.toString() + '?' + endpoint.name); 
    }

    /**
     * Gets the WSDL of this service.
     *
     * <p>
     * This WSDL will be compiled to generate client artifacts during a test.
     */
    @NotNull
    public List<URL> getWSDL() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        for (File w : war.getWSDL()) {
            urls.add(w.toURL());
        }
        return urls;
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
