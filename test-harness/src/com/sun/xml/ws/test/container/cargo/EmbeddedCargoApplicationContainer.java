package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.util.log.SimpleLogger;

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
    public EmbeddedCargoApplicationContainer(WsTool wsimport, WsTool wsgen, String containerId) {
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
        container.setClassLoader(World.runtime.getClassLoader());
    }

    @Override
    protected boolean copyRuntimeLibraries() {
        // runtime jars available in the container. no need to copy
        return false;
    }

    @Override
    protected boolean needsArchive() {
        // embedded tomcat doesn't need a war file
        return false;
    }

    public String toString() {
        return "EmbeddedContainer:"+container.getId();
    }
}
