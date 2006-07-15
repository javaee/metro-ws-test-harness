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
import java.io.FilenameFilter;
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

        // compile WSDL to generate client-side artifact
        context.parent.wsimport.invoke("-s", gensrcDir.getAbsolutePath(),
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
        String serviceClazzName = recurseClientArtifacts(classDir);

        if (serviceClazzName != null)
            context.serviceClass = cl.loadClass(serviceClazzName);
        else {
            throw new RuntimeException ("Cannot find the service class " + serviceClazzName);
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

    private String recurseClientArtifacts(File dir) throws Exception {

        File[] children = dir.listFiles();
        if (children != null) {
            for (File aChildren : children) {
                // Get filename of file or directory
                String serviceClazzName;

                if (aChildren.isDirectory()) {
                    serviceClazzName = recurseClientArtifacts(aChildren);

                } else {
                    serviceClazzName = checkFiles(aChildren.getParentFile());
                }
                if (serviceClazzName != null)
                    return serviceClazzName;
            }
        }
        return null;
    }

    public String checkFiles(File dir) throws Exception{

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".java");
            }
        };
        File[] javaFiles = dir.listFiles(filter);
        File serviceClazzFile = null;

        for (File sourceFile:javaFiles){

            BufferedReader reader = new BufferedReader(new FileReader(sourceFile
            ));
            String line;
            while ((line =reader.readLine()) != null){
                if (line.contains("extends Service")){
                    serviceClazzFile = sourceFile;
                    break;
                }
            }
        }
        String serviceClazzName = null;
        if (serviceClazzFile!=null){

            String fileName = serviceClazzFile.getAbsolutePath();
            //Remove .java from the fileName
            fileName = fileName.substring(0,fileName.lastIndexOf('.'));
            int o= fileName.indexOf("client-classes");
            //Get the package name for the file by taking a substring after
            // client-classes and replacing '/' by '.'
            serviceClazzName =  fileName.substring(o+15).replace(File.separatorChar,'.');
        }

        return serviceClazzName;

    }
}

