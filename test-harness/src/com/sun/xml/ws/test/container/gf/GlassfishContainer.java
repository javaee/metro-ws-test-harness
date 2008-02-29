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

package com.sun.xml.ws.test.container.gf;

import com.sun.enterprise.deployapi.SunDeploymentFactory;
import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.tool.WsTool;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.File;
import java.net.URL;

/**
 * {@link ApplicationContainer} implementation for Glassfish.
 *
 * Connection is made via JMX, so this works with GF running anywhere.
 *
 * @author Kohsuke Kawaguchi
 */
public final class GlassfishContainer extends AbstractApplicationContainer {

    private final DeploymentManager dm;
    private final Target[] targets;

    /**
     * HTTP address of the remote server where we access the application.
     */
    private final URL httpServerUrl;

    /**
     *
     * @param httpServerUrl
     *      URL of the HTTP server. This is where we access deployed applications.
     * @param host
     *      The host name of the JMX connection.
     * @param port
     *      The administration TCP port. Usually 4848.
     * @param userName
     *      Admin user name. Needed to connect to the admin port.
     * @param password
     *      Admin user password.
     */
    public GlassfishContainer(WsTool wsimport, WsTool wsgen, URL httpServerUrl, String host, int port, String userName, String password) throws Exception {
        super(wsimport, wsgen);

        this.httpServerUrl = httpServerUrl;

        System.out.println("Connecting to Glassfish");

        String connectionUri = "deployer:Sun:AppServer::"+host+":"+port;
        // to be more correct, we should load this from manifest.
        // but that requires a local glassfish installation
        dm = new SunDeploymentFactory().getDeploymentManager(connectionUri,userName,password);

        targets = dm.getTargets();
        if (targets.length == 0)
            throw new Exception("Can't find deployment targets for Glassfish");
    }


    public String getTransport() {
        return "http";
    }

    public void start() throws Exception {
        // noop. assumed to be running
    }

    public void shutdown() throws Exception {
        // noop. assumed to be running
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        String contextPath = service.service.getGlobalUniqueName();
        File archive = new File(service.workDir,contextPath+".war");

        createWARZip(service,archive);

        URL warURL = new URL(httpServerUrl, "/" + contextPath + "/");
        return new GlassfishApplication( warURL, service,this,deploy(archive,warURL));
    }

    /**
     * Deploys an application and returns the list of deployed module(s).
     */
    private TargetModuleID[] deploy(File war, URL targetUrl) throws Exception {
        System.out.println("Deploying a service to "+targetUrl);

        ProgressObject dpo = Monitor.join(
            dm.distribute(targets, war, null),"deployment failed");

        TargetModuleID[] modules = dpo.getResultTargetModuleIDs();

        Monitor.join(dm.start(modules),"failed to start services");

        return modules;
    }

    void undeploy(TargetModuleID[] modules, URL warURL) throws Exception {
        System.out.println("Undeploying a service from "+warURL);
        Monitor.join(dm.undeploy(modules),"undeploy operation failed");
    }


    /**
     * Monitors the asynchronous progress of the JSR-88 operation.
     */
    private static final class Monitor implements ProgressListener {

        public static ProgressObject join(ProgressObject po, String errorMessage) throws Exception {
            Monitor m = new Monitor();
            po.addProgressListener(m);
            m.join(errorMessage);
            return po;
        }

        private DeploymentStatus completionEvent;

        public synchronized void handleProgressEvent(ProgressEvent event) {
            DeploymentStatus s = event.getDeploymentStatus();
            if(s.isFailed() || s.isCompleted()) {
                completionEvent = s;
                notifyAll();
            }
        }

        /**
         * Wait till the asynchronous operation completes.
         */
        public synchronized void join(String errorMessage) throws Exception {
            while(completionEvent==null)
                wait();
            if(completionEvent.isFailed())
                throw new Exception(errorMessage+" : "+completionEvent.getMessage());
        }
    }
}
