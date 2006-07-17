package com.sun.xml.ws.test.container.local;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.util.CustomizationBean;
import com.sun.xml.ws.test.container.local.jelly.SunJaxwsInfoBean;
import com.sun.xml.ws.test.container.local.jelly.WebXmlInfoBean;
import com.sun.xml.ws.test.util.JavacTask;
import com.sun.xml.ws.test.util.ArgumentListBuilder;
import com.sun.xml.ws.test.tool.WsTool;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.XMLOutput;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ApplicationContainer} for the local transport.
 *
 * @author Kohsuke Kawaguchi
 */
public class LocalApplicationContainer implements ApplicationContainer {

    private final WsTool wsimport;
    private final WsTool wsgen;
    private final JellyContext jellyContext = new JellyContext();

    /**
     * Produce output for debugging the harness.
     */
    private final boolean debug;

    public LocalApplicationContainer(WsTool wscompile, WsTool wsgen, boolean debug) {
        this.wsimport = wscompile;
        this.wsgen = wsgen;
        this.debug = debug;
    }

    public void start() {
        // noop
    }

    public void shutdown() {
        // noop
    }

    @NotNull
    public Application deploy(DeployedService service) throws Exception {
        File wsdl = compileServer(service);
        prepareWarFile(service);
        return new LocalApplication(service,wsdl);
    }

    /**
     * Generates artifacts from WSDL (if any), then compile them
     * to javac, and invoke wsgen (if this is from java.)
     *
     * At the end of this method, we should have WSDL and compiled classes
     * regardless of the direction.
     *
     * The compiled class files will be stored inside {@link DeploymentContext#workDir}.
     *
     * @return
     *      the WSDL file (this is the one provided in the test data if this is "fromwsdl",
     *      and it is the generated one if the test is "fromjava".
     */
    private @NotNull File compileServer(DeployedService service) throws Exception {

        // Service starting from WSDL
        if(service.service.wsdl!=null) {
            // generate server artifacts from WSDL

            // Generate jaxws + jaxb binding customization file
            File serverCustomizationFile = genServerCustomizationFile(service);
            service.service.customizations.add(serverCustomizationFile);

            ArgumentListBuilder options = new ArgumentListBuilder();
            //Add customization files
            for (File custFile: service.service.customizations) {
                options.add("-b").add(custFile);
            }

            //Other options
            if(debug)
                options.add("-verbose");
            options.add("-s").add(service.workDir);
            options.add("-Xnocompile");
            options.add(service.service.wsdl);
            System.out.println("Generating server artifacts from " + service.service.wsdl);
            options.invoke(wsimport);
        }

        // both cases
        if(!wsimport.isNoop()) {
            JavacTask javac = new JavacTask();
            javac.setSourceDir(service.service.baseDir, service.workDir);
            javac.setDestdir(service.buildClassesDir);
            javac.execute();
        }

        // Service starting from Java
        if(service.service.wsdl==null) {
            // Use wsgen to generate the artifacts
            File wsdlDir = new File(service.webInfDir, "wsdl");
            wsdlDir.mkdirs();

            // for fromjava tests, we can't really support multiple endpoints in one service
            // because wsgen isn't capable of generating them into one WSDL.
            assert service.service.endpoints.size()==1;
            File generatedWsdl=null;

            for (TestEndpoint endpt : service.service.endpoints) {
                ArgumentListBuilder options = new ArgumentListBuilder();
                options.add("-wsdl");
                if(debug)
                    options.add("-verbose");
                options.add("-r").add(wsdlDir);
                String path = service.buildClassesDir.getAbsolutePath() + File.pathSeparatorChar + World.toolClasspath + File.pathSeparatorChar + World.runtimeClasspath;
                if(debug)
                    System.out.println("wsgen classpath arg = " + path);
                options.add("-cp").add(path);
                options.add("-s").add(service.buildClassesDir);
                options.add("-d").add(service.buildClassesDir);

                // obtain a report file from wsgen
                File report = new File(wsdlDir,"wsgen.report");
                options.add("-XwsgenReport").add(report);

                options.add(endpt.className);

                System.out.println("Generating WSDL");
                options.invoke(wsgen);

                // parse report
                Document dom = new SAXReader().read(report);
                generatedWsdl = new File(dom.getRootElement().elementTextTrim("wsdl"));
            }

            // patch this WSDL to point to the right local endpoint URL.
            // this WSDL is already placed in WEB-INF/wsdl, so no need for moving it
            patchWsdl(service, generatedWsdl, generatedWsdl);

            return generatedWsdl;
        } else {
            // patch the WSDL and copy it to WEB-INF/wsdl at the same time.
            File wsdlDir = new File(service.webInfDir,"wsdl");
            wsdlDir.mkdirs();

            File wsdl = service.service.wsdl;
            File dest = new File(wsdlDir, wsdl.getName());

            patchWsdl(service, wsdl,dest);

            return dest;
        }
    }

    /**
     * Prepares an exploded war file image for this service.
     */
    private void prepareWarFile(DeployedService service) throws Exception {

        generateSunJaxWsXml(service);
        generateWebXml(service);

        Project p = new Project();
        p.init();

        /*
            Jar warFile = new Jar();
            warFile.setProject(p);
            warFile.setDestFile(new File(context.workDir, service.parent.shortName + ".war"));
        */
        // warFile.setWebxml(new File(context.workDir, "web.xml"));

        Copy copy = new Copy();
        copy.setProject(p);
        FileSet classesSet = new FileSet();
        classesSet.setDir(service.buildClassesDir);
        copy.addFileset(classesSet);
        copy.setTodir(new File(service.webInfDir,"classes"));
        copy.execute();

        /* Not really necessary for local transport case
         *
            ZipFileSet  tmpZipFileSet = new ZipFileSet();
            tmpZipFileSet.setDir(new File(context.workDir,"WEB-INF"));
            //warFile.addWebinf(tmpZipFileSet);
            warFile.addZipfileset(tmpZipFileSet);
            warFile.execute();
        */

        // TODO: resources directory?

        // TODO: anything special needed when creating the manifest?
    }

    /**
     * This method uses Jelly to write the sun-jaxws.xml file. The
     * template file is sun-jaxws.jelly. The real work happens
     * in the SunJaxwsInfoBean object which supplies information
     * to the Jelly processor through accessor methods.
     *
     * @see com.sun.xml.ws.test.container.local.jelly.SunJaxwsInfoBean
     */
    private void generateSunJaxWsXml(DeployedService service) throws Exception {

        OutputStream outputStream =
            new FileOutputStream(new File(service.webInfDir, "sun-jaxws.xml"));
        XMLOutput output = XMLOutput.createXMLOutput(outputStream);
        SunJaxwsInfoBean infoBean = new SunJaxwsInfoBean(service);
        jellyContext.setVariable("data", infoBean);
        jellyContext.runScript(getClass().getResource("jelly/sun-jaxws.jelly"),
            output);
        output.flush();
    }

    /**
     * This method uses Jelly to write the web.xml file. The
     * template file is web.jelly. The real work happens
     * in the WebXmlInfoBean object which supplies information
     * to the Jelly processor through accessor methods.
     *
     * @see com.sun.xml.ws.test.container.local.jelly.WebXmlInfoBean
     */
    private void generateWebXml(DeployedService service) throws Exception {

        OutputStream outputStream =
            new FileOutputStream(new File(service.webInfDir, "web.xml"));
        XMLOutput output = XMLOutput.createXMLOutput(outputStream);
        WebXmlInfoBean infoBean = new WebXmlInfoBean(service.parent);
        jellyContext.setVariable("data", infoBean);
        jellyContext.runScript(getClass().getResource("jelly/web.jelly"),
            output);
        output.flush();
    }

    private File genServerCustomizationFile(DeployedService service) throws Exception {

        File serverCustomizationFile = new File(service.workDir, "custom-server.xml");
        OutputStream outputStream =
            new FileOutputStream(serverCustomizationFile);
        XMLOutput output = XMLOutput.createXMLOutput(outputStream);

        String packageName;
        if (service.service.name.equals("")) {
            packageName = service.service.parent.shortName;
        }
        else {
            packageName = service.service.parent.shortName + "." + service.service.name;
        }
        CustomizationBean infoBean = new CustomizationBean(packageName,
            service.service.wsdl.getCanonicalPath());
        jellyContext.setVariable("data", infoBean);
        jellyContext.runScript(getClass().getResource("jelly/custom-server.jelly"),
            output);
        output.flush();

        return serverCustomizationFile;
}
    /**
     * This is ugly but needed to be working quickly. A faster
     * way to do this would be to read in the old wsdl in stax
     * and write out the new one as we go. (TODO)
     *
     * Fix the address in the wsdl. This doesn't take into
     * account multiple endpoints, url patterns, etc. A
     * big TODO. Also, this could be a lot faster. To set
     * the ports and url patterns correctly, we would need
     * to parse the sun-jaxws.xml file, or keep the bean
     * around used by the Jelly code to query it. Maybe good
     * into to put in our memory model.
     */
    private void patchWsdl(DeployedService service, File src, File dest) throws Exception {
        Document doc = new SAXReader().read(src);
        List<Element> ports = doc.getRootElement().element("service").elements("port");

        for (Element port : ports) {
            String portName = port.attributeValue("name");

            Element address = (Element)port.elements().get(0);

            Attribute locationAttr = address.attribute("location");
            String newLocation =
                "local://" + service.workDir.getAbsolutePath() + "?" + portName;
            newLocation = newLocation.replace('\\', '/');
            locationAttr.setValue(newLocation);
        }

        // save file
        FileOutputStream os = new FileOutputStream(dest);
        new XMLWriter(os).write(doc);
        os.close();
    }

}
