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
