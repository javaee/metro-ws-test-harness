package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
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

        ConfigurationFactory configurationFactory =
            new DefaultConfigurationFactory();
        //File containerWorkDir = new File("c:\\sandbox\\tomcat");
        //containerWorkDir.mkdirs();
        LocalConfiguration configuration =
            (LocalConfiguration) configurationFactory.createConfiguration(
                containerId, ConfigurationType.STANDALONE );
                //containerWorkDir);

        configuration.setProperty(ServletPropertySet.PORT, Integer.toString(httpPort));
        configuration.setLogger(new SimpleLogger());
        // TODO: we should provide a mode to launch the container with debugger


        container = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(
            containerId, ContainerType.INSTALLED, configuration);
        container.setHome(homeDir);
    }

    public String toString() {
        return "CargoLocalContainer:"+container.getId();
    }
}
