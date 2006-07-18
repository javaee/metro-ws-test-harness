package com.sun.xml.ws.test.container.cargo;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.World;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.deployer.URLDeployableMonitor;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;

import java.net.URL;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractCargoContainer<C extends Container> extends AbstractApplicationContainer {

    protected final C container;

    protected AbstractCargoContainer(C container, WsTool wsimport, WsTool wsgen) {
        super(wsimport, wsgen);
        this.container = container;
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        String contextPath = service.service.getGlobalUniqueName();

        WAR assembly = assembleWar(service);

        // copy runtime classes into the classpath. this is slow.
        // isn't there a better way to do this?
        System.out.println("Copying runtime libraries");
        assembly.copyClasspath(World.runtimeClasspath);

        // TODO: fix Cargo so that it can work with exploded image, which is more efficient
        System.out.println("Assembling a war file");
        File archive = new File(service.workDir,contextPath+".war");
        assembly.zipTo(archive);

        org.codehaus.cargo.container.deployable.WAR war = (org.codehaus.cargo.container.deployable.WAR)
            new DefaultDeployableFactory().createDeployable(
                container.getId(), archive, DeployableType.WAR);

        war.setContext(contextPath);

        Deployer deployer = new DefaultDeployerFactory().createDeployer(container, DeployerType.REMOTE);

        URL serviceUrl = getServiceUrl(contextPath);

        System.out.println("Deploying a service to "+serviceUrl);
        deployer.deploy(war,new URLDeployableMonitor(serviceUrl));

        return new CargoApplication(
            deployer,
            war,
            serviceUrl,
            service);
    }

    protected abstract URL getServiceUrl(String contextPath) throws Exception;
}
