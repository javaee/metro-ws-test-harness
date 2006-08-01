package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.cargo.gf.GlassfishStandaloneLocalConfiguration;
import com.sun.xml.ws.test.container.cargo.gf.GlassfishInstalledLocalContainer;
import com.sun.xml.ws.test.container.cargo.gf.GlassfishInstalledLocalDeployer;
import com.sun.xml.ws.test.container.cargo.gf.GlassfishPropertySet;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.util.log.SimpleLogger;

import java.io.File;

/**
 * {@link ApplicationContainer} that launches a container from within the harness.
 *
 * <p>
 * This uses an image of the container installed locally, but
 * this operation does not affect the data file and configuration
 * files in that installation, so you need not have an installation
 * dedicated to this test harness.
 *
 * @author Kohsuke Kawaguchi
 */
public class InstalledCargoApplicationContainer extends AbstractRunnableCargoContainer<InstalledLocalContainer> {
    /**
     *
     * @param containerId
     *      The ID that represents the container. "tomcat5x" for Tomcat.
     * @param homeDir
     *      The installation of the container. For Tomcat, this is
     */
    public InstalledCargoApplicationContainer(WsTool wsimport, WsTool wsgen, String containerId, File homeDir) {
        super(wsimport,wsgen);

        // needed until glassfish becomes a part of Cargo
        ConfigurationFactory configurationFactory = new DefaultConfigurationFactory();
        configurationFactory.registerConfiguration("glassfish1x", ConfigurationType.STANDALONE, GlassfishStandaloneLocalConfiguration.class);
        DefaultContainerFactory containerFactory = new DefaultContainerFactory();
        containerFactory.registerContainer("glassfish1x", ContainerType.INSTALLED, GlassfishInstalledLocalContainer.class);
        deployerFactory.registerDeployer("glassfish1x", DeployerType.LOCAL, GlassfishInstalledLocalDeployer.class);

        // TODO: consider creating work directory to a visible place so that logs can be inspected.
        //File containerWorkDir = new File("c:\\sandbox\\tomcat");
        //containerWorkDir.mkdirs();
        LocalConfiguration configuration =
            (LocalConfiguration) configurationFactory.createConfiguration(
                containerId, ConfigurationType.STANDALONE );
                //containerWorkDir);

        configuration.setProperty(ServletPropertySet.PORT, Integer.toString(httpPort));
        configuration.setLogger(new SimpleLogger());

        // In case this is Glassfish, override all the other TCP ports
        // so that multiple test runs can co-exist on the same machine
        configuration.setProperty(GlassfishPropertySet.JMS_PORT,                Integer.toString(httpPort+1));
        configuration.setProperty(GlassfishPropertySet.IIOP_PORT,               Integer.toString(httpPort+2));
        configuration.setProperty(GlassfishPropertySet.HTTPS_PORT,              Integer.toString(httpPort+3));
        configuration.setProperty(GlassfishPropertySet.IIOPS_PORT,              Integer.toString(httpPort+4));
        configuration.setProperty(GlassfishPropertySet.IIOP_MUTUAL_AUTH_PORT,   Integer.toString(httpPort+5));
        configuration.setProperty(GlassfishPropertySet.JMX_ADMIN_PORT,          Integer.toString(httpPort+6));
        configuration.setProperty(GlassfishPropertySet.ADMIN_PORT,              Integer.toString(httpPort+7));

        // TODO: we should provide a mode to launch the container with debugger


        container = (InstalledLocalContainer) containerFactory.createContainer(
            containerId, ContainerType.INSTALLED, configuration);
        container.setHome(homeDir);
    }

    public String toString() {
        return "CargoLocalContainer:"+container.getId();
    }
}
