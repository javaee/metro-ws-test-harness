//
// HACKED VERSION FOR WS TEST HARNESS
//

// package devtests.deployment.util;
package gfdeployer;

import com.sun.enterprise.deployapi.SunDeploymentFactory;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author  administrator
 */
public class JSR88Deployer implements ProgressListener {

    private DeploymentManager dm;
    private Target[] targets;
    //private final String J2EE_DEPLOYMENT_MANAGER =
    //                    "J2EE-DeploymentFactory-Implementation-Class";

    /** Record all events delivered to this deployer (which is also a progress listener. */
    private Vector<ProgressEvent> receivedEvents = new Vector<ProgressEvent>();
    
    /** Record the TargetModuleIDs that resulted from the most recent operation. */
    private TargetModuleID [] mostRecentTargetModuleIDs = null;
    
    /** Creates a new instance of JSR88Deployer */
    public JSR88Deployer(String uri, String user, String password) throws DeploymentException {

        log("Connecting using uri = " + uri + "; user = " + user + "; password = " + password);

        try {
            // to be more correct, we should load this from manifest.
            dm = new SunDeploymentFactory().getDeploymentManager(uri,user,password);
        } catch (DeploymentManagerCreationException e) {
            throw new DeploymentException(e);
        }

        Target[] allTargets = dm.getTargets();
        if (allTargets.length == 0)
            throw new DeploymentException("Can't find deployment targets");

        // If test being run on EE, exclude the DAS server instance from the deploy targets
        String targetPlatform = System.getProperty("deploymentTarget");
        List<Target> filteredTargets = new ArrayList<Target>();
        if( ("SERVER".equals(targetPlatform)) || ("CLUSTER".equals(targetPlatform)) ) {
            for (Target allTarget : allTargets) {
                if (allTarget.getName().equals("server")) {
                    continue;
                }
                filteredTargets.add(allTarget);
            }
            targets = filteredTargets.toArray(new Target[filteredTargets.size()]);
        } else {
            targets = allTargets;
        }
    }

    private void waitTillComplete(ProgressObject po, String errorMessage) throws Exception {
        DeploymentStatus deploymentStatus;
        do {
            /*
             *The progress object may return a DeploymentStatus object that is a snapshot of the
             *current status and will never change.  (The spec is unclear on this behavior.)  So
             *to be sure, get a new deployment status every time through the loop to be sure of
             *getting the up-to-date status.
             */
            deploymentStatus = po.getDeploymentStatus();
            Thread.sleep(200);
        } while (!(deploymentStatus.isCompleted() || deploymentStatus.isFailed()));   

        if(deploymentStatus.isFailed())
            throw new DeploymentException(errorMessage);
    }

    public ProgressObject deploy(File archive, File deploymentPlan,
                                boolean startByDefault,
                                ModuleType type) 
        throws Exception {

        ProgressObject dpo;
        ProgressObject po;
        if (deploymentPlan == null) {
            log("Warning, deploying with null deployment plan");
            dpo = dm.distribute(targets, archive, null);
        } else {
            dpo = dm.distribute(targets, archive, deploymentPlan);
        }

        if (dpo!=null) {
            dpo.addProgressListener(this);
            waitTillComplete(dpo,"DEPLOY Action Failed");

            TargetModuleID[] targetModuleIDs = dpo.getResultTargetModuleIDs();        
            this.mostRecentTargetModuleIDs = targetModuleIDs;
            dumpResultModuleIDs("Deployed " , dpo);

            if (startByDefault) {
                log("STARTINNG... " + targetModuleIDs);
                po = dm.start(targetModuleIDs);
                if (po!=null) {
                    po.addProgressListener(this);
                    waitTillComplete(po,"START Action Failed");
                }
                return po;
            }
        }
        return(dpo);
    }

    public ProgressObject redeploy(String moduleID, File archive,
                                File deploymentPlan, boolean useStream) throws Exception {

        TargetModuleID[] list;
        if (moduleID == null) { //redeploy all but system apps
            throw new UnsupportedOperationException("DO NOT SUPPORT REDEPLOY MULTIPLE APPS");
        } else {
            list = findApplication(moduleID, null);
        }

        TargetModuleID[] modules = list;
        if (modules != null && modules.length > 0) {
            for (TargetModuleID module : modules) {
                log("REDEPLOYING... " + module);
            }
        }

        ProgressObject dpo;
        if (deploymentPlan == null || deploymentPlan.getName().equals("null")) {
            log("Warning, redeploying with null deployment plan");
            if (useStream) {            
                dpo = dm.redeploy(modules, 
                                    new FileInputStream(archive.getAbsolutePath()),
                                    null);
            } else {
                dpo = dm.redeploy(modules, archive, null);
            }
        } else {
            if (useStream) {            
                dpo = dm.redeploy(modules, 
                                    new FileInputStream(archive.getAbsolutePath()),
                                    new FileInputStream(deploymentPlan.getAbsolutePath()));
            } else {
                dpo = dm.redeploy(modules, archive, deploymentPlan);          
            }
        }            
        if (dpo!=null) {
            dpo.addProgressListener(this);
            waitTillComplete(dpo,"REDEPLOY Action Failed");

            this.mostRecentTargetModuleIDs = dpo.getResultTargetModuleIDs();
            dumpResultModuleIDs("Redeployed " , dpo);
        }
        return(dpo);
    }

    public ProgressObject stop(String moduleID) throws Exception {
        TargetModuleID[] list;
        if (moduleID == null) { //stop all but system apps
            list = getAllApplications(Boolean.TRUE);
        } else {
            list = findApplication(moduleID, Boolean.TRUE);
        }

        ProgressObject dpo = null;
        TargetModuleID[] modules = list;
        if (modules != null && modules.length > 0) {
            for (TargetModuleID module : modules) {
                log("STOPPING... " + module);
            }
            dpo = dm.stop(modules);
            if (dpo!=null) {
                dpo.addProgressListener(this);
                waitTillComplete(dpo,"STOP Action Failed");
                this.mostRecentTargetModuleIDs = dpo.getResultTargetModuleIDs();
            }
        }
        return(dpo);
    }

    /**
     *Starts an application, with an option of waiting between the time the operation is requested
     *and the time the deployer is added as a listener.  This option helps with the test for race
     *conditions involved with managing the list of listeners and the list of delivered events.
     */
    public ProgressObject start(String moduleID, int delayBeforeRegisteringListener) throws Exception {
        TargetModuleID[] list;
        if (moduleID == null) { //start all but system apps
            list = getAllApplications(Boolean.FALSE);
        } else {
            list = findApplication(moduleID, Boolean.FALSE);
        }

        TargetModuleID[] modules = list;
        ProgressObject dpo = null;
        if (modules != null && modules.length > 0) {
            for (TargetModuleID module : modules) {
                log("STARTINNG... " + module);
            }
            dpo = dm.start(modules);
            if (delayBeforeRegisteringListener > 0) {
                try {
                    log("Pausing before adding self as a progress listener");
                    Thread.sleep(delayBeforeRegisteringListener);
                } catch (InterruptedException ie) {
                    throw new RuntimeException(this.getClass().getName() + " was interrupted sleeping before adding itself as a progresslistener", ie);
                }
            }
            if (dpo!=null) {
                dpo.addProgressListener(this);
                if (delayBeforeRegisteringListener > 0) {
                    log("Now registered as a progress listener");
                }
                waitTillComplete(dpo,"START Action Failed");
                this.mostRecentTargetModuleIDs = dpo.getResultTargetModuleIDs();
            }
        }
        return(dpo);
    }

    public ProgressObject start(String moduleID) throws Exception {
        return start(moduleID, 0);
    }

    public ProgressObject undeploy(String moduleID) throws Exception {
        TargetModuleID[] list;
        //log ("000 trying to undeploy moduleID = " + moduleID);
        if (moduleID == null) { //undeploy all but system apps
            list = getAllApplications(null);
        } else {
            list = findApplication(moduleID, null);
        }

        ProgressObject dpo = null;
        TargetModuleID[] modules = list;
        if (modules != null && modules.length > 0) {
            for (TargetModuleID module : modules) {
                log("UNDEPLOYING... " + module);
            }
            dpo = dm.undeploy(modules);
            if (dpo!=null) {
                dpo.addProgressListener(this);
                waitTillComplete(dpo,"UNDEPLOY Action Failed");
                this.mostRecentTargetModuleIDs = dpo.getResultTargetModuleIDs();
            }
        }
        return(dpo);
    }

    protected void dumpResultModuleIDs(String prefix, ProgressObject po) {
        TargetModuleID[] targetModuleIDs = po.getResultTargetModuleIDs();
	dumpModulesIDs(prefix, targetModuleIDs);
    }

    protected void dumpModulesIDs(String prefix, TargetModuleID[] targetModuleIDs) {
        for (TargetModuleID targetModuleID : targetModuleIDs) {
            dumpModulesIDs(prefix, targetModuleID);
        }
    }
    
    protected void dumpModulesIDs(String prefix, TargetModuleID targetModuleID) {
        log(prefix + targetModuleID);
        TargetModuleID[] subs = targetModuleID.getChildTargetModuleID();
        if (subs!=null) {
            for (int i=0;i<subs.length;i++) {
                log(" Child " + i + " = " + subs[i]);
            }
        }
    }

    public TargetModuleID[] getApplications(ModuleType moduleType, Boolean running) 
        throws Exception {
        TargetModuleID[] modules;
        if (running==null) {
            modules = dm.getAvailableModules(moduleType, targets);
        } else if (running) {
            modules = dm.getRunningModules(moduleType, targets);
        } else {
            modules = dm.getNonRunningModules(moduleType, targets);
        }

        return modules;
    }

    public TargetModuleID[] getAllApplications(Boolean running)  throws Exception {
        //log("222. getAllApplications, running = " + running);
        TargetModuleID[] ears = getApplications(ModuleType.EAR, running);
        TargetModuleID[] wars = getApplications(ModuleType.WAR, running);
        TargetModuleID[] cars = getApplications(ModuleType.CAR, running);
        TargetModuleID[] ejbs = getApplications(ModuleType.EJB, running);
        TargetModuleID[] rars = getApplications(ModuleType.RAR, running);

        List list = new ArrayList();
        for (int i = 0; i < ears.length; i++) { list.add(ears[i]); }
        for (int i = 0; i < wars.length; i++) { list.add(wars[i]); }
        for (int i = 0; i < cars.length; i++) { list.add(cars[i]); }
        for (int i = 0; i < ejbs.length; i++) { list.add(ejbs[i]); }
        for (int i = 0; i < rars.length; i++) { list.add(rars[i]); }
    
        TargetModuleID[] results = new TargetModuleID[list.size()];
        for (int i = 0; i < list.size(); i++) { 
            results[i] = (TargetModuleID) list.get(i);
        }
        return results;
    }

    protected TargetModuleID[] findApplication(String moduleID, ModuleType moduleType, Boolean running) 
        throws Exception {
        /*
         *The DeploymentFacility requires that start, stop, redeploy, and undeploy requests
         *operate on the same set of targets that the original deployment did.  As written currently, this test
         *class always deploys to all available targets so the other functions should also
         *apply to all available targets.
         */
        TargetModuleID[] list = getApplications(moduleType, running);
        return filterTargetModuleIDsByModule(list, moduleID);
    }

    protected TargetModuleID[] findApplication(String moduleID, Boolean running) 
        throws Exception {
        //log("111 trying to get everything, moduleid = " + moduleID + "; boolean = " + running);
        TargetModuleID[] list = getAllApplications(running);
        return filterTargetModuleIDsByModule(list, moduleID);
    }

    /**
     *Filter an array of TargetModuleID, keeping only those that match the specified module ID.
     *@param tmids the array of TargetModuleID to be filtered
     *@param moduleID the name of the module of interest
     *@return new TargetModuleID array, containing only the elements from the original array that match the module ID
     */
    protected TargetModuleID[] filterTargetModuleIDsByModule(TargetModuleID [] tmids, String moduleID) {
        List<TargetModuleID> tmidsToUse = new ArrayList<TargetModuleID>();
        /*
         *Add to the vector of TMIDs each TMID from getApplications that also matches the
         *module ID.
         */
        for (TargetModuleID tmid : tmids) {
            if (moduleID.equals(tmid.getModuleID())) {
                tmidsToUse.add(tmid);
            }
        }
        return tmidsToUse.toArray(new TargetModuleID[tmidsToUse.size()]);
    }
    
    public void listApplications(ModuleType moduleType, Boolean running) 
        throws Exception {
        TargetModuleID[] modules = getApplications(moduleType, running); 
        if (modules == null) {
        } else
        for (TargetModuleID module : modules) {
            if (running == null) {
                dumpModulesIDs("    AVAILABLE ", module);
            } else if (running) {
                dumpModulesIDs("    RUNNING ", module);
            } else {
                dumpModulesIDs("    NOT RUNNING ", module);
            }
        }
    }
    
    /** Invoked when a deployment progress event occurs.
     *
     * @param event the progress status event.
     */
    public void handleProgressEvent(ProgressEvent event) {
        DeploymentStatus ds = event.getDeploymentStatus();
        System.out.println("Received Progress Event state " + ds.getState() + " msg = " + ds.getMessage());
        /*
         *Add this event to the vector collecting them.
         */
        this.receivedEvents.add(event);
    }

    /**
     *Report all the events received by this deployer.
     *
     *@return array of Objects containing the received events
     */
    public ProgressEvent [] getReceivedEvents() {
        ProgressEvent [] answer = new ProgressEvent[this.receivedEvents.size()];
        return this.receivedEvents.toArray(answer);
    }
    
    /**
     *Clear the collection of received events recorded by this deployer.
     */
    public void clearReceivedEvents() {
        this.receivedEvents.clear();
    }

    public TargetModuleID [] getMostRecentTargetModuleIDs() {
        return this.mostRecentTargetModuleIDs;
    }
    
    public void clearMostRecentTargetModuleIDs() {
        this.mostRecentTargetModuleIDs = null;
    }
    
    private static void usage() {
        System.out.println("Usage: command <JSR88.URI> <admin-user> <admin-password> [command-parameters]");
        System.out.println("    where command is one of [deploy<-stream> | redeploy<-stream> | undeploy | start | stop | list]");
        System.out.println("    where <JSR88.URI> is: deployer:Sun:AppServer::${admin.host}:${admin.port} for PE");
        System.out.println("    where <JSR88.URI> is: deployer:Sun:AppServer::${admin.host}:${admin.port}:https for EE");
        System.out.println("    where command-parameters are as follows");
        System.out.println("      deploy: <startByDefault> <archiveFile> [<deploymentFile>]");
        System.out.println("      deploy-stream: <startByDefault> <archiveFile> [<deploymentFile>]");
        System.out.println("      deploy-stream-withtype: <startByDefault> <archiveFile> <moduleType> [<deploymentFile>]");
        System.out.println("      redeploy: <moduleID> <archiveFile> [<deploymentFile>]");
        System.out.println("      redeploy-stream: <moduleID> <archiveFile> [<deploymentFile>]");
        System.out.println("      undeploy: [all | moduleID]");
        System.out.println("      start: [all | moduleID]");
        System.out.println("      stop: [all | moduleID]");
        System.out.println("      list: [all | running | nonrunning]");
    }

    public static void log(String message) {
        System.out.println("[JSR88Deployer]:: " + message);
    }

    public static void error(String message) {
        System.err.println("[JSR88Deployer]:: " + message);
    }
}
