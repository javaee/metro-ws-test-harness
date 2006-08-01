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

    protected final DefaultDeployerFactory deployerFactory = new DefaultDeployerFactory();


    protected AbstractCargoContainer(WsTool wsimport, WsTool wsgen) {
        super(wsimport, wsgen);
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

        WAR war = (WAR)new DefaultDeployableFactory().createDeployable(
            container.getId(), archive, DeployableType.WAR);

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
