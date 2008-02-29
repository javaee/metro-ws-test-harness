/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.model.TestService;
import com.sun.xml.ws.test.util.ArgumentListBuilder;
import com.sun.xml.ws.test.util.JavacTask;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
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
        super("Deploy "+context.service.name,context.parent);
        this.context = context;
    }

    public void runBare() throws Throwable {
        // deploy the service
        context.app = context.parent.container.deploy(context);
        //For STS we do not want to generate client artifacts
        if (!context.service.isSTS) {
            // then use that WSDL to generate client
            generateClientArtifacts();
        } else {
            addSTSToClasspath();
            // updateWsitClient();
        }
    }
/*
    public void updateWsitClient()throws Exception {
        File wsitClientFile = new File(context.service.parent.resources,"wsit-client.xml");
        if (wsitClientFile.exists() ){
            SAXReader reader = new SAXReader();
            Document document = reader.read(wsitClientFile);
            Element root = document.getRootElement();
            Element policy = root.element("Policy");
            Element sts = policy.element("ExactlyOne").element("All").element("PreconfiguredSTS");

            Attribute  endpoint = sts.attribute("endpoint");
            URI uri = context.app.getEndpointAddress((TestEndpoint)context.service.endpoints.toArray()[0]);
            endpoint.setValue(uri.toString());

            Attribute wsdlLoc = sts.attribute("wsdlLocation");
            wsdlLoc.setValue(context.service.wsdl.wsdlFile.toURI().toString());

            XMLWriter writer = new XMLWriter(new FileWriter(wsitClientFile));
            writer.write( document );
            writer.close();

        } else {
            throw new RuntimeException("wsit-client.xml is absent. It is required. \n"+
                    "Please check " + context.service.parent.resources );
        }


    }
*/

    public void addSTSToClasspath() throws Exception{
        List<URL> classpath = context.clientClasspaths;

        ClassLoader baseCl = World.runtime.getClassLoader();
        if (context.parent.clientClassLoader != null) {
            baseCl = context.parent.clientClassLoader;
        }
        
        classpath.add(new File(context.warDir, "WEB-INF/classes").toURL());

        context.parent.clientClassLoader= new URLClassLoader( classpath.toArray(new URL[classpath.size()]), baseCl );

    }
    /**
     * Generate & compile source files from service WSDL.
     */
    private void generateClientArtifacts() throws Exception {

        File gensrcDir = makeWorkDir("client-source");
        File classDir = makeWorkDir("client-classes");

        for (URL wsdl : context.app.getWSDL()) {
            ArgumentListBuilder options = new ArgumentListBuilder();
            // Generate cusomization file & add as wsimport option

            // we used to do this just to set the package name, but
            // it turns out we can do it much easily with the -p option
            //options.add("-b");
            //options.add(genClientCustomizationFile(context).getAbsolutePath());

            // set package name. use 'client' to avoid collision between server artifacts
            options.add("-p").add(context.parent.descriptor.name +".client");
            options.add("-extension");

            //Add user's additional customization files
            for (File custFile : context.parent.descriptor.clientCustomizations)
                options.add("-b").add(custFile);

            //Other options
            if(World.debug)
                options.add("-verbose");
            options.add("-s").add(gensrcDir);
            options.add("-d").add(classDir);
            options.add("-Xnocompile");
            options.add(wsdl);
            if(World.debug)
                System.out.println("wsdl = " + wsdl);
            options.addAll(context.service.parent.wsimportClientOptions);
            // compile WSDL to generate client-side artifact
            options.invoke(context.parent.wsimport);
        }

        // compile the generated source files to javac
        JavacTask javac = new JavacTask();
        javac.setSourceDir(
            gensrcDir,
            context.parent.descriptor.common,
            new File(context.parent.descriptor.home,"client")
        );
        javac.setDestdir(classDir);
        javac.setDebug(true);
        if(!context.parent.wsimport.isNoop())
            // if we are just reusing the existing artifacts, no need to recompile.
            javac.execute();

        // load the generated classes
        List<URL> classpath = context.clientClasspaths;
        classpath.add(classDir.toURL());
        // TODO: only the local container needs server classes in the classloader.
        classpath.add(new File(context.warDir, "WEB-INF/classes").toURL());
        if(context.service.parent.resources!=null)
            classpath.add(context.service.parent.resources.toURL());

        /**
         * If there is a service like STS it has already been added to context.parent.clientClassLoader
         *  add that to the final classpath
         */
        if (context.parent.clientClassLoader instanceof URLClassLoader) {
            URL [] urls = ((URLClassLoader)context.parent.clientClassLoader).getURLs();
            classpath.addAll(Arrays.asList(urls));
        }

        ClassLoader cl = new URLClassLoader( classpath.toArray(new URL[classpath.size()]),
                World.runtime.getClassLoader() );

        context.parent.clientClassLoader = cl;

        // The following code scans the generated source files and look for the class
        // that extend from Service. This could be as simple as
        // line-by-line scan of "extends Service" ---
        // if we want to be more robust, we can write an AnnotationProcessor
        // so that we can work on top of Java AST, but this simple grep-like
        // approach would work just fine with wsimport.

        List<String> serviceClazzNames = new ArrayList<String>();
        findServiceClass(gensrcDir,serviceClazzNames);

        if (serviceClazzNames.isEmpty())
            System.out.println("WARNING: Cannot find the generated 'service' class that extends from javax.xml.ws.Service. Assuming provider-only service");

        for (String name : serviceClazzNames)
            context.serviceClass.add(cl.loadClass(name));
    }

    /**
     * Creates another test to be exeucted at the end
     * to undeploy the service that this test deployed.
     */
    public Executor createUndeployer() {
        return new Executor("Undeploy "+context.service.name,context.parent) {
            public void runBare() throws Throwable {
                if(DeploymentExecutor.this.context.app!=null)
                    DeploymentExecutor.this.context.app.undeploy();
            }
        };
    }

    /**
     * Recursively scans the Java source directory and find a class
     * that extends from "Service", add them to the given list.
     */
    private void findServiceClass(File dir,List<String> result) throws Exception {
        OUTER:
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                findServiceClass(child,result);
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
                        result.add(pkg+'.'+ className);

                        continue OUTER;
                    }
                }
                reader.close();
            }
        }
    }

    /**
     * Generates a JAX-WS customization file for generating client artifacts.
     */
    //private File genClientCustomizationFile(DeployedService service) throws Exception {
    //    File customizationFile = new File(service.workDir, "custom-client.xml");
    //    OutputStream outputStream =
    //        new FileOutputStream(customizationFile);
    //    XMLOutput output = XMLOutput.createXMLOutput(outputStream);
    //
    //    String packageName = service.service.parent.shortName;
    //
    //    // to avoid collision between the client artifacts and server artifacts
    //    // when testing everything inside a single classloader (AKA local transport),
    //    // put the client artifacts into a different package.
    //    CustomizationBean infoBean = new CustomizationBean(packageName+".client",
    //                                        service.app.getWSDL().toExternalForm());
    //    JellyContext jellyContext = new JellyContext();
    //    jellyContext.setVariable("data", infoBean);
    //    jellyContext.runScript(getClass().getResource("custom-client.jelly"),output);
    //    output.flush();
    //
    //    return customizationFile;
    //}
}

