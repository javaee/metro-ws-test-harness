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

import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.RemoteContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.RuntimeConfiguration;
import org.codehaus.cargo.container.property.RemotePropertySet;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

import java.net.URL;

/**
 * {@link ApplicationContainer} that talks to a server that's already running
 * (IOW launched outside this harness.)
 *
 * <p>
 * This implementation requires that the container be launched externally first.
 * Then the harness will simply deploy/undeloy by using this running container.
 * Useful for repeatedly debugging a test with a remote container.
 *
 * @author Kohsuke Kawaguchi
 */
public class RemoteCargoApplicationContainer extends AbstractCargoContainer<RemoteContainer> {

    private final URL serverUrl;

    /**
     *
     * @param containerId
     *      The ID that represents the container. "tomcat5x" for Tomcat.
     * @param server
     * @param userName
     *      The user name of the admin. Necessary to deploy a war remotely
     * @param password
     *      The password of the admin. Necessary to deploy a war remotely
     */
    public RemoteCargoApplicationContainer(WsTool wsimport, WsTool wsgen, String containerId, URL server, String userName, String password, boolean httpspi) throws Exception {
        super(wsimport,wsgen,httpspi);

        this.serverUrl = server;

        ConfigurationFactory configurationFactory =
            new DefaultConfigurationFactory();
        RuntimeConfiguration configuration =
            (RuntimeConfiguration) configurationFactory.createConfiguration(
                containerId, ContainerType.REMOTE, ConfigurationType.RUNTIME);

        configuration.setProperty(RemotePropertySet.USERNAME, userName);
        configuration.setProperty(RemotePropertySet.PASSWORD, password);
        if(containerId.startsWith("tomcat"))
            configuration.setProperty(TomcatPropertySet.MANAGER_URL,
                new URL(server,"/manager").toExternalForm());

        // TODO: we should provide a mode to launch the container with debugger

        super.container = (RemoteContainer) new DefaultContainerFactory().createContainer(
            containerId, ContainerType.REMOTE, configuration);
    }

    protected URL getServiceUrl(String contextPath) throws Exception {
        return new URL(serverUrl,"/"+contextPath+"/");
    }

    public void start() throws Exception {
        // the container is assumed to be started
        // noop
    }

    public void shutdown() throws Exception {
        // noop.
    }

    public String toString() {
        return "CargoRemoteContainer:"+container.getId();
    }
}
