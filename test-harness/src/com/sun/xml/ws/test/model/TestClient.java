package com.sun.xml.ws.test.model;

import com.sun.istack.NotNull;
import com.sun.istack.test.VersionProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * If true, it indicates that this test doesn't leave any side-effect
     * on the client or the server. With a proper option,
     * multiple instances of the same test will run in parallel to test
     * the behaviors in the concurrent environment.
     */
    public final boolean sideEffectFree;

    public TestClient(TestDescriptor parent, VersionProcessor applicableVersions, Script script, boolean sideEffectFree) {
        this.parent = parent;
        this.applicableVersions = applicableVersions;
        this.script = script;
        this.sideEffectFree = sideEffectFree;
    }
}
