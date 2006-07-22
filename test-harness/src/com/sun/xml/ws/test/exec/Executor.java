package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.container.DeploymentContext;
import junit.framework.TestCase;

/**
 * Executes a part of a test.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Executor extends TestCase {
    /**
     * Every {@link Executor} works for one {@link DeploymentContext}.
     */
    public final DeploymentContext context;

    protected Executor(String name, DeploymentContext context) {
        super(context.descriptor.name+"."+name);
        this.context = context;
    }

    /**
     * Executes something.
     *
     * Error happened during this will be recorded as a test failure.
     */
    public abstract void runBare() throws Throwable;
}
