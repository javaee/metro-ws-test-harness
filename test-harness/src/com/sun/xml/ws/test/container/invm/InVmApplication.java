package com.sun.xml.ws.test.container.invm;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Application} implementation for {@link InVmContainer}.
 *
 * @author ken
 */
final class InVmApplication implements Application {

    private final @NotNull
    WAR war;

    /**
     * "local://path/to/exploded/dir" portion of the endpoint address.
     * Adding "?portName" makes it the full endpoint address.
     */
    private final @NotNull
    URI baseEndpointAddress;

    /**
     * <tt>InVmServer</tt> object. This is loaded in another classloader,
     * so we can't use a typed value.
     */
    private final @NotNull Object server;

    /** Creates a new instance of LocalApplication */
    InVmApplication(@NotNull WAR war, Object server, URI endpointAddress) {
        this.war = war;
        this.server = server;
        this.baseEndpointAddress = endpointAddress;
    }

    /**
     * Returns the actual endpoint address to which the given {@link TestEndpoint}
     * is deployed.
     */
    @NotNull
    public URI getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception {
        // I'm not too confident if endpoint.name is always the port local name.
        return baseEndpointAddress.resolve('?'+endpoint.name);
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
        InterpreterEx i = new InterpreterEx(server.getClass().getClassLoader());
        i.set("server",server);
        i.eval("server.undeploy()");
    }
}
