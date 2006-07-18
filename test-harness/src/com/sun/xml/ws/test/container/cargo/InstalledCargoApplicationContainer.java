package com.sun.xml.ws.test.container.cargo;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;

import java.io.File;
import java.util.Random;

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
public class InstalledCargoApplicationContainer extends AbstractApplicationContainer {
    private final String containerId;

    private InstalledLocalContainer container;

    /**
     *
     * @param containerId
     *      The ID that represents the container. "tomcat5x" for Tomcat.
     * @param homeDir
     *      The installation of the container. For Tomcat, this is
     *      <tt>$TOMCAT_HOME</tt>.
     */
    public InstalledCargoApplicationContainer(WsTool wsimport, WsTool wsgen, boolean debug, String containerId, File homeDir) {
        super(wsimport,wsgen,debug);

        this.containerId = containerId;

        ConfigurationFactory configurationFactory =
            new DefaultConfigurationFactory();
        LocalConfiguration configuration =
            (LocalConfiguration) configurationFactory.createConfiguration(
                containerId, ConfigurationType.STANDALONE);

        // set TCP port to somewhere between 20000-30000
        configuration.setProperty(ServletPropertySet.PORT, Integer.toString(new Random().nextInt(10000)+20000));
        // TODO: we should provide a mode to launch the container with debugger


        container = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(
            containerId, ContainerType.INSTALLED, configuration);
        container.setHome(homeDir);
    }

    public void start() throws Exception {
        container.start();
    }

    public void shutdown() throws Exception {
        container.stop();
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        Deployable war = new DefaultDeployableFactory().createDeployable(
            containerId, assembleWar(service).root, DeployableType.WAR);

        Deployer deployer = new DefaultDeployerFactory().createDeployer(container, DeployerType.LOCAL);

        deployer.deploy(war);

        return new CargoApplication(deployer,war);
    }

    public String toString() {
        return "CargoContainer:"+containerId;
    }
}
