package com.sun.xml.ws.test.container.cargo.gf;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.property.RemotePropertySet;
import org.codehaus.cargo.container.spi.deployer.AbstractLocalDeployer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class GlassfishInstalledLocalDeployer extends AbstractLocalDeployer {
    public GlassfishInstalledLocalDeployer(InstalledLocalContainer localContainer) {
        super(localContainer);
    }

    private GlassfishInstalledLocalContainer getLocalContainer() {
        return (GlassfishInstalledLocalContainer)super.getContainer();
    }
    private GlassfishStandaloneLocalConfiguration getConfiguration() {
        return (GlassfishStandaloneLocalConfiguration) getLocalContainer().getConfiguration();
    }

    public DeployerType getType() {
        return DeployerType.LOCAL;
    }

    public void deploy(Deployable deployable) {
        doDeploy(deployable,false);
    }

    public void redeploy(Deployable deployable) {
        doDeploy(deployable,true);
    }

    private void doDeploy(Deployable deployable, boolean overwrite) {
        List<String> args = new ArrayList<String>();
        args.add("deploy");
        if(overwrite)
            args.add("--force");
        if(deployable instanceof WAR) {
            args.add("--contextroot");
            args.add(((WAR)deployable).getContext());
        }

        addConnectOptions(args);

        args.add(deployable.getFile().getAbsolutePath());

        getLocalContainer().invokeAsAdmin(false, args.toArray(new String[args.size()]));
    }

    public void undeploy(Deployable deployable) {
        List<String> args = new ArrayList<String>();
        args.add("undeploy");

        addConnectOptions(args);

        // not too sure how asadmin determines 'name'
        args.add(cutExtension(deployable.getFile().getName()));

        getLocalContainer().invokeAsAdmin(false, args.toArray(new String[args.size()]));
    }

    public void start(Deployable deployable) {
        // TODO
        super.start(deployable);
    }

    public void stop(Deployable deployable) {
        // TODO
        super.stop(deployable);
    }

    private String cutExtension(String name) {
        int idx = name.lastIndexOf('.');
        if(idx>=0)  return name.substring(0,idx);
        else        return name;
    }

    private void addConnectOptions(List<String> args) {
        args.add("--interactive=false");
        args.add("--port");
        args.add(getConfiguration().getPropertyValue(GlassfishPropertySet.ADMIN_PORT));
        args.add("--user");
        args.add(getConfiguration().getPropertyValue(RemotePropertySet.USERNAME));
        args.add("--passwordfile");
        args.add(getConfiguration().getPasswordFile().getAbsolutePath());
    }
}
