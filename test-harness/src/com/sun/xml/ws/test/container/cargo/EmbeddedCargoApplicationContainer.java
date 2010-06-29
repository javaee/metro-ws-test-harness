/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.AbstractFactoryRegistry;
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
    public EmbeddedCargoApplicationContainer(WsTool wsimport, WsTool wsgen, String containerId, int port, boolean httpspi) {
        super(wsimport,wsgen,port,httpspi);

        ConfigurationFactory configurationFactory =
            new DefaultConfigurationFactory(AbstractFactoryRegistry.class.getClassLoader());
        LocalConfiguration configuration =
            (LocalConfiguration) configurationFactory.createConfiguration(
                containerId, ContainerType.EMBEDDED, ConfigurationType.STANDALONE );

        configuration.setProperty(ServletPropertySet.PORT, Integer.toString(httpPort));
        configuration.setLogger(new SimpleLogger());
        // TODO: we should provide a mode to launch the container with debugger


        container = (EmbeddedLocalContainer) new DefaultContainerFactory(AbstractFactoryRegistry.class.getClassLoader()).createContainer(
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
