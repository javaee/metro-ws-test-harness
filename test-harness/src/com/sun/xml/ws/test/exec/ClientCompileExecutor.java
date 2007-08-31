package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.util.JavacTask;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Used to compile clients when there's no server to deploy.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClientCompileExecutor extends Executor {
    public ClientCompileExecutor(DeploymentContext context) {
        super("Compile clients "+context.descriptor.name, context);
    }

    public void runBare() throws Throwable {
        File classDir = makeWorkDir("client-classes");

        // compile the generated source files to javac
        JavacTask javac = new JavacTask();
        javac.setSourceDir(
            context.descriptor.common,
            new File(context.descriptor.home,"client")
        );
        javac.setDestdir(classDir);
        javac.setDebug(true);
        if(!context.wsimport.isNoop())
            // if we are just reusing the existing artifacts, no need to recompile.
            javac.execute();

        // load the generated classes
        ClassLoader cl = new URLClassLoader( new URL[]{classDir.toURL()},
                World.runtime.getClassLoader() );

        context.clientClassLoader = cl;
    }
}
