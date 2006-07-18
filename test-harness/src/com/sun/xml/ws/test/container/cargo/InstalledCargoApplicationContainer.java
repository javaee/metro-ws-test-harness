package com.sun.xml.ws.test.container.cargo;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;
import org.codehaus.cargo.util.log.SimpleLogger;

import java.io.File;
import java.net.URL;
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

    private final int httpPort;

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
        //File containerWorkDir = new File("c:\\sandbox\\tomcat");
        //containerWorkDir.mkdirs();
        LocalConfiguration configuration =
            (LocalConfiguration) configurationFactory.createConfiguration(
                containerId, ConfigurationType.STANDALONE );
                //containerWorkDir);

        // set TCP port to somewhere between 20000-30000
        httpPort = new Random().nextInt(10000) + 20000;
        configuration.setProperty(ServletPropertySet.PORT, Integer.toString(httpPort));
        configuration.setLogger(new SimpleLogger());
        // TODO: we should provide a mode to launch the container with debugger


        container = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(
            containerId, ContainerType.INSTALLED, configuration);
        container.setHome(homeDir);
    }

    public void start() throws Exception {
        System.out.println("Starting "+containerId);
        container.start();
    }

    public void shutdown() throws Exception {
        System.out.println("Stopping "+containerId);
        container.stop();
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        WAR assembly = assembleWar(service);
        org.codehaus.cargo.container.deployable.WAR war = (org.codehaus.cargo.container.deployable.WAR)
            new DefaultDeployableFactory().createDeployable(containerId, assembly.root, DeployableType.WAR);
        String contextPath = service.service.getGlobalUniqueName();
        war.setContext(contextPath);

        Deployer deployer = new DefaultDeployerFactory().createDeployer(container, DeployerType.LOCAL);

        URL serviceUrl = new URL("http", "localhost", httpPort, "/" + contextPath + "/");

        System.out.println("Deploying a service to "+serviceUrl);
        deployer.deploy(war);

        return new CargoApplication(
            deployer,
            war,
            serviceUrl,
            service);
    }

    public String toString() {
        return "CargoContainer:"+containerId;
    }
}
