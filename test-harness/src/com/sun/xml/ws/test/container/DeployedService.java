package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.model.TestService;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * Information about running {@link TestService}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class DeployedService {

    /**
     * The {@link DeploymentContext} that owns this service.
     */
    public final @NotNull DeploymentContext parent;

    /**
     * Service that was deployed.
     */
    public final @NotNull TestService service;

    /**
     * {@link Application} that represents the currently deployed service
     * on the container.
     *
     * <p>
     * This field is set when a service is deployed.
     */
    public Application app;

    /**
     * Root of the working directory to store things related to this service.
     */
    public final @NotNull File workDir;

    /**
     * Directory to store a war file image.
     */
    public final @NotNull File warDir;

    /**
     * Classpaths to load client artifacts for this service.
     */
    public final List<URL> clientClasspaths = new ArrayList<URL>();

    /**
     * The class that represents the generated <tt>Service</tt> class.
     *
     * This field is set to non-null when the service is deployed
     * and client artifacts are generated.
     */
    public Class serviceClass;

    /*package*/ DeployedService(DeploymentContext parent, TestService service) {
        this.parent = parent;
        this.service = service;

        // create work directory
        String rel = "services";
        if(service.name.length()>0)
            rel += '/' + service.name;
        this.workDir = new File(parent.workDir,rel);

        this.warDir = new File(workDir,"war");
    }

    /**
     * Creates working directory
     */
    /*package*/ void prepare() {
        warDir.mkdirs();
    }
}
