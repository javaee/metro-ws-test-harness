package com.sun.xml.ws.test.container.cargo.gf;

import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfigurationCapability;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.property.RemotePropertySet;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
final class GlassfishStandaloneLocalConfigurationCapability extends AbstractStandaloneLocalConfigurationCapability {
    /**
     * Configuration-specific supports Map.
     */
    private final Map supportsMap = new HashMap();

    /**
     * Initialize the configuration-specific supports Map.
     */
    GlassfishStandaloneLocalConfigurationCapability()
    {
        // unsupported
        supportsMap.put(GeneralPropertySet.LOGGING, Boolean.FALSE);
        supportsMap.put(GeneralPropertySet.LOGGING, Boolean.FALSE);
        supportsMap.put(ServletPropertySet.USERS, Boolean.FALSE);

        // recognize those
        supportsMap.put(RemotePropertySet.USERNAME, Boolean.TRUE);
        supportsMap.put(RemotePropertySet.PASSWORD, Boolean.TRUE);
        supportsMap.put(GeneralPropertySet.HOSTNAME, Boolean.TRUE);
        supportsMap.put(GeneralPropertySet.JVMARGS, Boolean.TRUE);

        // this.defaultSupportsMap.put(ServletPropertySet.PORT, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     * @see AbstractStandaloneLocalConfigurationCapability#getPropertySupportMap()
     */
    protected Map getPropertySupportMap()
    {
        return this.supportsMap;
    }
}
