package com.sun.xml.ws.test;

/**
 * How we use wsgen.
 *
 * @author Kohsuke Kawaguchi
 */
public enum WsGenMode {
    /**
     * Invokes wsgen for eager wrapper bean generation.
     * This is the default.
     */
    ALWAYS,
    /**
     * Don't generate wrapper beans (the harness still actually executes wsgen
     * for its own purpose but the wrapper beans will be discarded.)
     */
    IGNORE,
    /**
     * Test both scenarios.
     */
    BOTH,
}
