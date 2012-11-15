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

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.tool.WsTool;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.deployer.URLDeployableMonitor;
import org.codehaus.cargo.generic.AbstractFactoryRegistry;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;

import java.io.File;
import java.net.URL;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractCargoContainer<C extends Container> extends AbstractApplicationContainer {

    /**
     * Expected to be set by the constructor of the derived class.
     * Conceptually final --- no update after that.
     */
    protected C container;

    protected final DefaultDeployerFactory deployerFactory = new DefaultDeployerFactory(AbstractFactoryRegistry.class.getClassLoader());
    protected final DefaultDeployableFactory deployableFactory = new DefaultDeployableFactory(AbstractFactoryRegistry.class.getClassLoader());


    protected AbstractCargoContainer(WsTool wsimport, WsTool wsgen, boolean httpspi) {
        super(wsimport, wsgen, httpspi);
    }

    public String getTransport() {
        return "http";
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        String contextPath = service.service.getGlobalUniqueName();
        File archive;

        if(needsArchive()) {
            archive = new File(service.workDir,contextPath+".war");
            createWARZip(service,archive);
        } else {
            archive = assembleWar(service).root;
        }

        WAR war = (WAR)deployableFactory.createDeployable(
            container.getId(), archive.getAbsolutePath(), DeployableType.WAR);

        war.setContext(contextPath);

        Deployer deployer = deployerFactory.createDeployer(container, DeployerType.toType(container.getType()));

        URL serviceUrl = getServiceUrl(contextPath);

        System.out.println("Verifying that "+serviceUrl+" is already removed");
        try {
            deployer.undeploy(war);
        } catch (Exception e) {
            // swallow any failure to undeploy
        }
        System.out.println("Deploying a service to "+serviceUrl);
        deployer.deploy(war,new URLDeployableMonitor(serviceUrl));

        return new CargoApplication( deployer, war, serviceUrl, service);
    }

    protected abstract URL getServiceUrl(String contextPath) throws Exception;

    /**
     * True if the Cargo implementation only takes a .war file
     * and not the exploded war image.
     *
     * Not creating a war file makes the testing faster.
     */
    protected boolean needsArchive() {
        return true;
    }
}
