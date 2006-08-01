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

//
// these names are named to match asadmin --domainproperties option
//

    /**
     * JMS port. Defaults to 7676.
     */
    String JMS_PORT = "cargo.glassfish.jms.port";

    /**
     * IIOP port. Defaults to 3700.
     */
    String IIOP_PORT = "cargo.glassfish.orb.listener.port";

    /**
     * HTTPS port. Defaults to 8181.
     */
    String HTTPS_PORT = "cargo.glassfish.http.ssl.port";

    /**
     * IIOP+SSL port. Defaults to 3820.
     */
    String IIOPS_PORT = "cargo.glassfish.orb.ssl.port";

    /**
     * IIOP mutual authentication port. Defaults to 3920.
     */
    String IIOP_MUTUAL_AUTH_PORT = "cargo.glassfish.orb.mutualauth.port";

    /**
     * JMX admin port. Defaults to 8686.
     */
    String JMX_ADMIN_PORT = "cargo.glassfish.domain.jmxPort";
}
