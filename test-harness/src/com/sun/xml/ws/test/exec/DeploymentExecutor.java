package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.model.TestService;
import com.sun.xml.ws.test.util.JavacWrapper;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * {@link TestCase} that deploys a {@link TestService} to
 * the {@link ApplicationContainer}.
 *
 * <p>
 * After this test is run, {@link #createUndeployer()} needs
 * to be used to undeploy the deployed service.
 *
 * @author Kohsuke Kawaguchi
 */
public class DeploymentExecutor extends Executor {
    private final DeployedService context;

    public DeploymentExecutor(DeployedService context) {
        super("Deploy "+context.service,context.parent);
        this.context = context;
    }

    public void runBare() throws Throwable {
        // deploy the service
        context.app = context.parent.container.deploy(context);
        // then use that WSDL to generate client
        generateClientArtifacts();
    }

    private void generateClientArtifacts() throws Exception {
        File gensrcDir = makeWorkDir("client-source");
        File classDir = makeWorkDir("client-classes");

        // compile WSDL to generate client-side artifacts.
        // TODO for ken: replace this hard-coded client customization file with the generated one
        context.parent.wsimport.invoke("-s", gensrcDir.getAbsolutePath(),

            // customization file
            "-b",
            new File(context.parent.descriptor.home,"custom-client.xml").getAbsolutePath(),

            "-Xnocompile", context.app.getWSDL().getPath() );

        // compile the generated source files to javac
        JavacWrapper javacWrapper = new JavacWrapper();
        javacWrapper.init(gensrcDir.getAbsolutePath(), classDir.getAbsolutePath());
        javacWrapper.execute();

        // load the generated classes
        List<URL> classpath = context.clientClasspaths;
        classpath.add(classDir.toURL());
        classpath.add(new File(context.webInfDir, "classes").toURL());
        if(context.service.parent.resources!=null)
            classpath.add(context.service.parent.resources.toURL());

        ClassLoader cl = new URLClassLoader( classpath.toArray(new URL[0]),
                World.runtime.getClassLoader() );

        context.parent.clientClassLoader = cl;

        // The following code scans the generated source files and look for the class
        // that extend from Service. This could be as simple as
        // line-by-line scan of "extends Service" ---
        // if we want to be more robust, we can write an AnnotationProcessor
        // so that we can work on top of Java AST, but this simple grep-like
        // approach would work just fine with wsimport.

        // I believe there's only one such class, but will check with Jitu
        // whether a single wsimport invocation may create multiple Service classes
        //Class c = Class.forName(sourceFile.getAbsolutePath());
        //context.serviceClass = c; // TODO: set the discovered service class here
        String serviceClazzName = findServiceClass(gensrcDir);

        if (serviceClazzName != null)
            context.serviceClass = cl.loadClass(serviceClazzName);
        else {
            throw new RuntimeException ("Cannot find the generated 'service' class that extends from javax.xml.ws.Service");
        }

    }

    private File makeWorkDir(String dirName) {
        File gensrcDir = new File(context.parent.workDir, dirName);
        gensrcDir.mkdirs();
        return gensrcDir;
    }

    /**
     * Creates another test to be exeucted at the end
     * to undeploy the service that this test deployed.
     */
    public Executor createUndeployer() {
        return new Executor("Undeploying "+context.service,context.parent) {
            public void runBare() throws Throwable {
                if(DeploymentExecutor.this.context.app!=null)
                    DeploymentExecutor.this.context.app.undeploy();
            }
        };
    }

    /**
     * Recursively scans the Java source directory and find a class
     * that extends from "Service".
     *
     * @return
     *      fully qualified class name.
     */
    private String findServiceClass(File dir) throws Exception {
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                String serviceClazzName = findServiceClass(child);
                if(serviceClazzName!=null)
                    return serviceClazzName; // found it
            } else
            if (child.getName().endsWith(".java")) {
                // check if this is the class that extends from "Service"

                BufferedReader reader = new BufferedReader(new FileReader(child));
                String pkg = null;  // this variable becomes Java package of this source file
                String line;
                while ((line =reader.readLine()) != null){
                    if(line.startsWith("package ")) {
                        pkg = line.substring(8,line.indexOf(';'));
                    }
                    if (line.contains("extends Service")){
                        // found it.
                        reader.close();

                        String className = child.getName();
                        // remove .java from the fileName
                        className = className.substring(0,className.lastIndexOf('.'));
                        //Get the package name for the file by taking a substring after
                        // client-classes and replacing '/' by '.'
                        return pkg+'.'+ className;
                    }
                }
                reader.close();
            }
        }
        return null;
    }
}

