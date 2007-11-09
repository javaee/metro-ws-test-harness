package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.Realm;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.jelly.EndpointInfoBean;
import com.sun.xml.ws.test.container.jelly.SunJaxwsInfoBean;
import com.sun.xml.ws.test.container.jelly.WebXmlInfoBean;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.util.ArgumentListBuilder;
import com.sun.xml.ws.test.util.FileUtil;
import com.sun.xml.ws.test.util.JavacTask;
import com.sun.xml.ws.test.util.Jelly;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.Path;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents an exploded WAR file on a file system.
 *
 * This class primarily contains operations that generate various files that
 * constitute a war file.
 */
public final class WAR {
    /**
     * The root directory of the war image.
     */
    public final File root;

    /**
     * "build/classes" directory under the work directory. It is used
     * often enough that it is created here to avoid typo errors.
     */
    public final File classDir;

    /**
     * "WEB-INF" directory under the work directory. It is used
     * often enough that it is created here to avoid typo errors.
     */
    public final File webInfDir;

    /**
     * "WEB-INF/lib" directory.
     */
    public final File libDir;

    /**
     * Directory to put additional generated source files.
     */
    public final File srcDir;

    /**
     * One web application may end up having multiple WSDLs if a fromjava service
     * contains multiple @WebService classes.
     */
    private final List<File> wsdl = new ArrayList<File>();

    /**
     * This war file is created for this service.
     */
    public final DeployedService service;


    public WAR(DeployedService service) {
        this.service = service;
        root = service.warDir;
        webInfDir = new File(root,"WEB-INF");
        classDir = new File(webInfDir,"classes");
        classDir.mkdirs();
        libDir = new File(webInfDir,"lib");
        libDir.mkdir();
        srcDir = new File(service.workDir,"gen-src");
        srcDir.mkdir();
    }

    /**
     * Creates a war archive from the exploded image at {@link #root}.
     */
    public void zipTo(File archive) throws Exception {
        Zip zip = new Zip();
        zip.setProject(World.project);
        zip.setDestFile(archive);
        zip.setBasedir(root);
        zip.execute();
    }

    /**
     * Copies the classpath specified by the given {@link Path}
     * into <tt>WEB-INF/lib</tt> and <tt>WEB-INF/classes</tt>
     */
    public void copyClasspath(Realm classpath) throws Exception {
        int n = 0;
        for (File path : classpath.list()) {
            if(path.isFile())
                // just copy one jar
                FileUtil.copyFile(path,new File(libDir,path.getName()));
            else {
                // create an uncompressed jar file. This serves a few purposes.
                //  - in general file systems are not good at dealing with many small files
                //  - we'll do the archiving anyway when we pack this into a jar
                Jar jar = new Jar();
                jar.setProject(World.project);
                jar.setDestFile(new File(libDir,"generated"+(n++)+".jar"));
                jar.setBasedir(path);
                jar.setCompress(false);
                jar.execute();
            }
        }
    }

    /**
     * Copies handler files in to WEB-INF/classes
     */
    public void copyHandlerChainFiles(File[] handlerConfigs) {
        for (File config : handlerConfigs) {
            FileUtil.copyFile(config, new File(classDir, config.getName()));
        }
    }

    /**
     * Gets the path of the WSDL.
     *
     * <p>
     * This is either copied from the test data (for "fromwsdl" tests),
     * or generated (for "fromjava" tests.) For fromjava tests with
     * multiple <tt>WebService</tt> classes, you may get more than one WSDLs.
     *
     * <p>
     * In a situation where there's no WSDL and just provider service,
     * the list may be empty.
     */
    public @NotNull List<File> getWSDL() {
        return wsdl;
    }

    /**
     * This method uses Jelly to write the sun-jaxws.xml file. The
     * template file is sun-jaxws.jelly. The real work happens
     * in the SunJaxwsInfoBean object which supplies information
     * to the Jelly processor through accessor methods.
     *
     * @see SunJaxwsInfoBean
     *
     * @return
     *      list of endpoints that were discovered.
     */
    final List<EndpointInfoBean> generateSunJaxWsXml() throws Exception {
        Jelly jelly = new Jelly(getClass(),"jelly/sun-jaxws.jelly");
        SunJaxwsInfoBean infoBean = new SunJaxwsInfoBean(this);
        jelly.set("data", infoBean);
        jelly.run(new File(webInfDir, "sun-jaxws.xml"));

        return infoBean.getEndpointInfoBeans();
    }

    /**
     * This method uses Jelly to write the web.xml file. The
     * template file is web.jelly. The real work happens
     * in the WebXmlInfoBean object which supplies information
     * to the Jelly processor through accessor methods.
     *
     * @see WebXmlInfoBean
     */
    final void generateWebXml(List<EndpointInfoBean> endpoints) throws Exception {
        Jelly jelly = new Jelly(getClass(),"jelly/web.jelly");
        WebXmlInfoBean infoBean = new WebXmlInfoBean(service.parent,endpoints);
        jelly.set("data", infoBean);
        jelly.run(new File(webInfDir, "web.xml"));
    }

    /**
     * Generates <tt>sun-web.xml</tt>
     */
    final void generateSunWebXml() throws Exception {
        Jelly jelly = new Jelly(getClass(),"jelly/sun-web.jelly");
        jelly.run(new File(webInfDir, "sun-web.xml"));
    }

    /**
     * Generate server artifacts from WSDL.
     */
    final void compileWSDL(WsTool wsimport) throws Exception {
        assert service.service.wsdl!=null;

        ArgumentListBuilder options = new ArgumentListBuilder();
        //Add customization files
        for (File custFile: service.service.customizations) {
            options.add("-b").add(custFile);
        }
        options.add("-extension");
        options.add("-p").add(service.service.getGlobalUniqueName());

        //Other options
        if(World.debug)
            options.add("-verbose");
        options.add("-s").add(srcDir);
        options.add("-d").add(classDir);
        options.add("-Xnocompile");
        options.add(service.service.wsdl.wsdlFile);
        options.addAll(service.service.parent.wsimportServerOptions);
        if(!wsimport.isNoop()) {
            System.out.println("Generating server artifacts from " + service.service.wsdl.wsdlFile);
            options.invoke(wsimport);
        }

        // copy WSDL into a war file
        File wsdlDir = new File(webInfDir,"wsdl");
        wsdlDir.mkdirs();

        File src = service.service.wsdl.wsdlFile;
        assert this.wsdl.isEmpty();
        File wsdlFile = new File(wsdlDir, src.getName());
        this.wsdl.add(wsdlFile);

        FileUtil.copyFile(src,wsdlFile);
        for (File importedWsdl :service.service.wsdl.importedWsdls){
            FileUtil.copyFile(importedWsdl,new File(wsdlDir,importedWsdl.getName()));
        }
        for (File schema :service.service.wsdl.schemas){
            FileUtil.copyFile(schema,new File(wsdlDir,schema.getName()));
        }
    }

    /**
     * Compiles Java source files into <tt>WEB-INF/classes</tt>.
     */
    final void compileJavac() throws Exception {
        JavacTask javac = new JavacTask();
        if(service.parent.descriptor.common != null)
            javac.setSourceDir(service.service.baseDir, srcDir,
                    service.parent.descriptor.common );
        else
            javac.setSourceDir(service.service.baseDir, srcDir);

        javac.setDestdir(classDir);
        javac.setDebug(true);
        javac.execute();
    }

    /**
     * Generates a WSDL into a war file if this is "fromjava" service.
     */
    final void generateWSDL(WsTool wsgen) throws Exception {
        assert service.service.wsdl==null;

        // Use wsgen to generate the artifacts
        File wsdlDir = new File(webInfDir, "wsdl");
        wsdlDir.mkdirs();

        for (TestEndpoint endpt : service.service.endpoints) {
            if(endpt.isProvider)    continue;
            ArgumentListBuilder options = new ArgumentListBuilder();
            options.add("-wsdl");
            if(World.debug)
                options.add("-verbose");
            options.add("-r").add(wsdlDir);
            Path cp = new Path(World.project);
            cp.createPathElement().setLocation(classDir);
            cp.add(World.tool.getPath());
            cp.add(World.runtime.getPath());
            if(World.debug)
                System.out.println("wsgen classpath arg = " + cp);
            options.add("-cp").add(cp);
            options.add("-s").add(classDir);
            options.add("-d").add(classDir);

            // obtain a report file from wsgen
            File report = new File(wsdlDir,"wsgen.report");
            options.add("-XwsgenReport").add(report);

            options.add(endpt.className);

            System.out.println("Generating WSDL");
            if(World.debug)
                System.out.println(options);
            options.invoke(wsgen);

            // parse report
            Document dom = new SAXReader().read(report);
            wsdl.add(new File(dom.getRootElement().elementTextTrim("wsdl")));
        }
    }
}
