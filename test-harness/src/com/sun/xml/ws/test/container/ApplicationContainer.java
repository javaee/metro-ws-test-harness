package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;

/**
 * Represents a place that runs services.
 *
 * <p>
 * This object needs to be multi-thread safe.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface ApplicationContainer {
    /**
     * Returns the transport that this container uses for testing.
     *
     * @return
     *      For example, "http", "local", "tcp", "jms", etc. It should match
     *      the scheme portion of
     *      the endpoint address URI. Never null. This value is compared in
     *      the descriptor's transport declaration to decide wheter to run a
     *      test or not.
     */
    String getTransport();

    /**
     * Starts the container.
     *
     * This is invoked at the very beginning before
     * any service is deployed.
     */
    void start() throws Exception;

    /**
     * Starts a service inside a container, making it ready
     * to process requests.
     */
    @NotNull Application deploy(DeployedService service) throws Exception;

    /**
     * Stops the container.
     *
     * This is invoked at the end to clean up any resources.
     */
    void shutdown() throws Exception;
}
