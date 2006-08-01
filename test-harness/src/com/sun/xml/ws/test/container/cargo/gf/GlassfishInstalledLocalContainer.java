package com.sun.xml.ws.test.container.cargo.gf;

import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Java;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.spi.AbstractInstalledLocalContainer;
import org.codehaus.cargo.util.CargoException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class GlassfishInstalledLocalContainer extends AbstractInstalledLocalContainer {
    public GlassfishInstalledLocalContainer(LocalConfiguration localConfiguration) {
        super(localConfiguration);
    }

    /**
     * Invokes asadmin
     */
    /*package*/ void invokeAsAdmin(String... args) {
        File exec = getAsadminExecutable();

        List<String> cmds = new ArrayList<String>();
        cmds.add(exec.getAbsolutePath());
        for (String arg : args) {
            cmds.add(arg);
        }

        try {
            Execute exe = new Execute();
            exe.setCommandline(cmds.toArray(new String[0]));
            int exitCode = exe.execute();
            if(exitCode!=0)
                // the first token is the command
                throw new CargoException(args[0]+" failed. asadmin exited "+exitCode);
        } catch (IOException e) {
            throw new CargoException("Failed to invoke asadmin",e);
        }
    }

    private File getAsadminExecutable() {
        File home = getHome();
        if(home==null || !home.exists())
            throw new CargoException("Glassfish home directory is not set");

        File exec;

        if(File.pathSeparatorChar==';') {
            // on Windows
            exec = new File(home,"bin/asadmin.bat");
        } else {
            // on other systems
            exec = new File(home,"bin/asadmin");
        }

        if(!exec.exists())
            throw new CargoException("asadmin command not found at "+exec);

        return exec;
    }

    protected void doStart(Java java) throws Exception {
        getConfiguration().configure(this);

        invokeAsAdmin("start-domain",
            "--interactive=false",
            "--domaindir",
            getConfiguration().getHome().getAbsolutePath(),
            "cargo-domain"
            );
    }

    protected void doStop(Java java) throws Exception {
        invokeAsAdmin("stop-domain",
            "--interactive=false",
            "--port",
            getConfiguration().getPropertyValue(GlassfishPropertySet.ADMIN_PORT),
            "cargo-domain"
            );
    }

    public String getId() {
        return "glassfish1x";
    }

    public String getName() {
        return "Glassfish v1";
    }

    public ContainerCapability getCapability() {
        return CAPABILITY;
    }

    private static final ContainerCapability CAPABILITY = new GlassfishContainerCapability();
}
