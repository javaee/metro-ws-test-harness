package com.sun.xml.ws.test.model;

import com.sun.istack.Nullable;
import com.sun.istack.NotNull;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * A service to be deployed for a test.
 *
 * <p>
 * TODO: We need to be able to mark service as an EJB service
 *
 * @author Kohsuke Kawaguchi
 */
public class TestService {

    /**
     * Name of the service.
     *
     * The name must be:
     * <ol>
     *  <li>Unique within {@link TestDescriptor}
     *  <li>an empty string or a valid Java identifier.
     * </ol>
     *
     * <p>
     * An empty string is convenient to describe the default/primary service
     * (or when there's just one service involved, which is the majority.)
     */
    @NotNull
    public final String name;

    /**
     * Directory in which the service's source files reside.
     */
    @NotNull
    public final File baseDir;
    
    /**
     * Optional WSDL file that describes this service.
     */
    @Nullable
    public final File wsdl;

    /**
     * Possibly empty list of JAXB/JAX-WS external binding customizations.
     *
     * Must be empty when {@link #wsdl} is null.
     */
    @NotNull
    public final List<File> customizations = new ArrayList<File>();

    /**
     * {@link TestEndpoint}s that this service exposes.
     *
     * <p>
     * The harness uses this information to inject proxies to the client.
     */
    @NotNull
    public final Set<TestEndpoint> endpoints = new LinkedHashSet<TestEndpoint>();

    public final TestDescriptor parent;

    public TestService(TestDescriptor parent, String name, File baseDir, File wsdl) {
        this.parent = parent;
        this.name = name;
        this.wsdl = wsdl;
        this.baseDir = baseDir;
    }

    public String toString() {
        return name+" of "+parent.toString();
    }
}
