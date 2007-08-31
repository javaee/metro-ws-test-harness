package com.sun.xml.ws.test;

import junit.framework.TestCase;

import java.io.File;

/**
 * A test case that will always fail.
 *
 * @author Kohsuke Kawaguchi
 */
class FailedTest extends TestCase {
    private final Throwable t;

    public FailedTest(String name, Throwable t) {
        super(name);
        this.t = t;
    }

    public FailedTest(File f, Throwable t) {
        this(f.getPath(),t);
    }

    protected void runTest() throws Throwable {
        throw new Exception(t);
    }
}
