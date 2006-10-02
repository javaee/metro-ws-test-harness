package com.sun.xml.ws.test.model;

/**
 * Set of transports.
 *
 * @author Kohsuke Kawaguchi
 */
public interface TransportSet {
    /**
     * Checks if the given transport is contained in this set.
     */
    boolean contains(String transport);

    /**
     * Constant that represents a set that includes everything.
     */
    public static final TransportSet ALL = new TransportSet() {
        public boolean contains(String transport) {
            return true;
        }
    };

    /**
     * {@link TransportSet} that consists of a single value.
     */
    public static final class Singleton implements TransportSet {
        private final String transport;

        public Singleton(String transport) {
            this.transport = transport;
        }

        public boolean contains(String transport) {
            return this.transport.equals(transport);
        }
    }
}
