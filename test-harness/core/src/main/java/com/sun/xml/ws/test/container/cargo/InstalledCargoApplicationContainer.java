/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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
import com.sun.xml.ws.test.container.cargo.gf.GlassfishPropertySet;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.util.FileUtil;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.AbstractFactoryRegistry;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;

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
    public InstalledCargoApplicationContainer(WsTool wsimport, WsTool wsgen, String containerId, File homeDir, int port, boolean httpspi) throws IOException {
        super(wsimport,wsgen,port,httpspi);

        // needed until glassfish becomes a part of Cargo

        ConfigurationFactory configurationFactory = new DefaultConfigurationFactory(AbstractFactoryRegistry.class.getClassLoader());
        // For tomcat local, cargo doesn't copy shared/lib jars to working dir
        // configurationFactory.registerConfiguration("tomcat5x", ContainerType.INSTALLED, ConfigurationType.STANDALONE, Tomcat5xMetroStandaloneLocalConfiguration.class);

        DefaultContainerFactory containerFactory = new DefaultContainerFactory(AbstractFactoryRegistry.class.getClassLoader());

        File containerWorkDir = FileUtil.createTmpDir(true);
        containerWorkDir.mkdirs();
        System.out.println("Container working directory: "+containerWorkDir);

        LocalConfiguration configuration =
            (LocalConfiguration) configurationFactory.createConfiguration(
                containerId, ContainerType.INSTALLED, ConfigurationType.STANDALONE,
                containerWorkDir.getAbsolutePath());

        configuration.setProperty(ServletPropertySet.PORT, Integer.toString(httpPort));
//        configuration.setLogger(new SimpleLogger());

        // In case this is Glassfish, override all the other TCP ports
        // so that multiple test runs can co-exist on the same machine
        configuration.setProperty(GlassfishPropertySet.JMS_PORT,                getPort());
        configuration.setProperty(GlassfishPropertySet.IIOP_PORT,               getPort());
        configuration.setProperty(GlassfishPropertySet.HTTPS_PORT,              getPort());
        configuration.setProperty(GlassfishPropertySet.IIOPS_PORT,              getPort());
        configuration.setProperty(GlassfishPropertySet.IIOP_MUTUAL_AUTH_PORT,   getPort());
        configuration.setProperty(GlassfishPropertySet.JMX_ADMIN_PORT,          getPort());
        configuration.setProperty(GlassfishPropertySet.ADMIN_PORT,              getPort());

        if (containerId.startsWith("tomcat")) {
            configuration.setProperty(TomcatPropertySet.AJP_PORT, getPort());
        }

        // TODO: we should provide a mode to launch the container with debugger

        container = (InstalledLocalContainer) containerFactory.createContainer(
            containerId, ContainerType.INSTALLED, configuration);
        container.setHome(homeDir.getAbsolutePath());
        container.setOutput(containerWorkDir.getAbsolutePath() + File.separatorChar + "server.log");

        Map<String, String> props = new HashMap<String, String>();
        props.put("java.endorsed.dirs", System.getProperty("java.endorsed.dirs"));
        props.put("WSIT_HOME", System.getProperty("WSIT_HOME"));
        container.setSystemProperties(props);

        for (File f : World.runtime.list()) {
            if (f.getName().endsWith(".jar")) {
                container.addExtraClasspath(f.getAbsolutePath());
            } else {
                if ("classes".equals(f.getName())) {
                    String fName = f.getParentFile().getParentFile().getName();
                    File jar = new File(new File(System.getProperty("java.io.tmpdir")), fName + System.currentTimeMillis() + ".jar");
                    jar.deleteOnExit();
                    Zip zip = new Zip();
                    zip.setProject(new Project());
                    zip.setDestFile(jar);
                    zip.setBasedir(f);
                    zip.execute();
                    container.addExtraClasspath(jar.getAbsolutePath());
                }
            }
        }

    }

    /**
     * For tomcat local, since cargo doesn't support copying of shared/lib jars,
     * the war can be created with the jars in the WEB-INF/lib. Other option is
     * to subclass Tomcat5xStandaloneLocalConfiguration and do the copying of
     * jars from Tomcat installation to local working dir in doConfigure()
     *
     * Copy JAX-WS runtime code?
     *
    protected boolean copyRuntimeLibraries() {
        return false;
    }
     */

    @Override
    public String toString() {
        return "CargoLocalContainer:"+container.getId();
    }

    private String getPort() {
        return String.valueOf(AbstractRunnableCargoContainer.getFreePort());
    }
}
