package com.sun.xml.ws.test.container.cargo.gf;

import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfiguration;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.RemotePropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.util.DefaultFileHandler;
import org.codehaus.cargo.util.CargoException;

import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class GlassfishStandaloneLocalConfiguration extends AbstractStandaloneLocalConfiguration {

    private static final ConfigurationCapability CAPABILITY =
        new GlassfishStandaloneLocalConfigurationCapability();

    /**
     *
     * @param home
     *      The work directory where files needed to run Glassfish will be created.
     */
    public GlassfishStandaloneLocalConfiguration(File home) {
        super(home);

        // default properties
        setProperty(RemotePropertySet.USERNAME, "admin");
        setProperty(RemotePropertySet.PASSWORD, "adminadmin");
        setProperty(GeneralPropertySet.HOSTNAME, "localhost");
        setProperty(GlassfishPropertySet.ADMIN_PORT, "4848");

        // ServletPropertySet.PORT default set to 8080 by the super class
    }

    public ConfigurationCapability getCapability() {
        return CAPABILITY;
    }

    /**
     * Creates a new domain and set up the workspace by invoking the "asadmin" command.
     */
    protected void doConfigure(LocalContainer container) throws Exception {
        DefaultFileHandler fileHandler = new DefaultFileHandler();
        fileHandler.delete(getHome());


        String password = getPropertyValue(RemotePropertySet.PASSWORD);
        if(password.length()<8)
            throw new CargoException("password needs to be 8 characters or longer");

        ((GlassfishInstalledLocalContainer)container).invokeAsAdmin(
            "create-domain",
            "--interactive=false",
            "--adminport",
            getPropertyValue(GlassfishPropertySet.ADMIN_PORT),
            "--adminuser",
            getPropertyValue(RemotePropertySet.USERNAME),
            "--adminpassword",
            password,
            "--instanceport",
            getPropertyValue(ServletPropertySet.PORT),

            // it looks like domain name can be anything, but check with the dev
            "cargo-domain");
    }

}
