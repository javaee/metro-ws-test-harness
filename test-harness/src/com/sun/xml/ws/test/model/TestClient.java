package com.sun.xml.ws.test.model;

import com.sun.istack.NotNull;
import com.sun.istack.test.VersionProcessor;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * A test script that plays the role of the client.
 *
 * TODO: needs to support clients that run inside a container (for testing Transaction)
 *
 * @author Kohsuke Kawaguchi
 */
public class TestClient {
    /**
     * Versions to which this client test applies.
     */
    @NotNull
    public final VersionProcessor applicableVersions;

    /**
     * The BeanShell script to be executed.
     */
    @NotNull
    public final Script script;

    /**
     * {@link TestDescriptor} to which this {@link TestClient} belongs.
     */
    @NotNull
    public final TestDescriptor parent;

     /**
     * Possibly empty list of JAXB/JAX-WS external binding customizations.
     *
     *
     */
    @NotNull
    public final List<File> customizations = new ArrayList<File>();

    public TestClient(TestDescriptor parent, VersionProcessor applicableVersions, Script script) {
        this.parent = parent;
        this.applicableVersions = applicableVersions;
        this.script = script;
    }
}
