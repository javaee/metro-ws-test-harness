package com.sun.xml.ws.test.container.cargo.gf;

/**
 * Interface for Glassfish-specific properties.
 *
 */
public interface GlassfishPropertySet
{
    /**
     * The admin HTTP port that Glassfish will use.
     * Defaults to 4848.
     */
    String ADMIN_PORT = "cargo.glassfish.adminPort";
}
