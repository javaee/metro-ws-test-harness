package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.RealmBuilder;
import com.sun.xml.ws.test.World;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.util.log.SimpleLogger;

import java.io.File;
import java.io.IOException;

/**
 * {@link ApplicationContainer} that loads the container into the harness VM.
 *
 * <p>
 * This mode still requires the local installation of the container, to load
 * jar files from.
 *
 * @author Kohsuke Kawaguchi
 */
public class EmbeddedCargoApplicationContainer extends AbstractRunnableCargoContainer<EmbeddedLocalContainer> {
    /**
     *
     * @param containerId
     *      The ID that represents the container. "tomcat5x" for Tomcat.
     * @param homeDir
     *      The installation of the container. For Tomcat, this is
     */
    public EmbeddedCargoApplicationContainer(WsTool wsimport, WsTool wsgen, String containerId, File homeDir) throws IOException {
        super(wsimport,wsgen);

        ConfigurationFactory configurationFactory =
            new DefaultConfigurationFactory();
        LocalConfiguration configuration =
            (LocalConfiguration) configurationFactory.createConfiguration(
                containerId, ConfigurationType.STANDALONE );

        configuration.setProperty(ServletPropertySet.PORT, Integer.toString(httpPort));
        configuration.setLogger(new SimpleLogger());
        // TODO: we should provide a mode to launch the container with debugger


        container = (EmbeddedLocalContainer) new DefaultContainerFactory().createContainer(
            containerId, ContainerType.EMBEDDED, configuration);

        // TODO: we need an abstraction to work with multiple containers
        // and their different jar layouts.
        // this is for Tomcat 5.x
        RealmBuilder builder = new RealmBuilder(World.container);
        builder.addJarFolder(new File(homeDir,"bin"));
        builder.addJarFolder(new File(homeDir,"common/lib"));
        builder.addJarFolder(new File(homeDir,"server/lib"));
    }


    public String toString() {
        return "EmbeddedContainer:"+container.getId();
    }
}
