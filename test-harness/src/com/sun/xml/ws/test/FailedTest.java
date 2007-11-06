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

// this turns out to be a bad idea because in this way the full path name will appear as the test suite name,
// which messes up <junitreport> because it tries to use that as the package name. 
//    public FailedTest(File f, Throwable t) {
//        this(f.getPath(),t);
//    }

    protected void runTest() throws Throwable {
        throw new Exception(t);
    }
}
