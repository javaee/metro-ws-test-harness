package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.istack.NotNull;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.Container;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractCargoContainer<C extends Container> extends AbstractApplicationContainer {

    protected final C container;

    protected AbstractCargoContainer(C container, WsTool wsimport, WsTool wsgen, boolean debug) {
        super(wsimport, wsgen, debug);
        this.container = container;
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        WAR assembly = assembleWar(service);
        org.codehaus.cargo.container.deployable.WAR war = (org.codehaus.cargo.container.deployable.WAR)
            new DefaultDeployableFactory().createDeployable(
                container.getId(), assembly.root, DeployableType.WAR);

        String contextPath = service.service.getGlobalUniqueName();
        war.setContext(contextPath);

        Deployer deployer = new DefaultDeployerFactory().createDeployer(container, DeployerType.REMOTE);

        URL serviceUrl = getServiceUrl(contextPath);

        System.out.println("Deploying a service to "+serviceUrl);
        deployer.deploy(war);

        return new CargoApplication(
            deployer,
            war,
            serviceUrl,
            service);
    }

    protected abstract URL getServiceUrl(String contextPath) throws Exception;
}
