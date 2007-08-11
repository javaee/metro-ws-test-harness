package com.sun.xml.ws.test.model;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.istack.test.VersionProcessor;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.Main;
import com.sun.xml.ws.test.model.TransportSet.Singleton;
import com.sun.xml.ws.test.client.ScriptBaseClass;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.exec.ClientExecutor;
import com.sun.xml.ws.test.exec.DeploymentExecutor;
import com.sun.xml.ws.test.exec.PrepareExecutor;
import com.sun.xml.ws.test.exec.ConcurrentClientExecutor;
import com.sun.xml.ws.test.exec.JavaClientExecutor;
import com.sun.xml.ws.test.tool.WsTool;
import com.thaiopensource.relaxng.jarv.RelaxNgCompactSyntaxVerifierFactory;
import junit.framework.TestSuite;
import org.apache.tools.ant.types.FileSet;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.iso_relax.jaxp.ValidatingSAXParserFactory;
import org.iso_relax.verifier.Schema;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.*;

/**
 * Root object of the test model. Describes one test.
 *
 * <p>
 * TODO: Transaction needs some 'beforeTest'/'afterTest' hook to clean up database
 *
 * @author Kohsuke Kawaguchi
 */
public class TestDescriptor {

    /**
     * A Java identifier that represents a name of this test.
     *
     * <p>
     * A test name needs to be unique among all the tests.  It will be
     * generated by the test harness dynamically from the test's partial
     * directory path. Namely, from toplevel test case directory to the
     * specific test case directory.
     * Something like this 'testcases.policy.parsing.someSpecificTest'
     *
     * This token is used by the harness to avoid collision when
     * running multiple tests in parallel (for example, this can be
     * used as a web application name so that multiple test services can be
     * deployed on the same container without interference.)
     *
     */
    @NotNull
    public final String name;

    /**
     * If non-null, this directory contains resources files used for tests.
     *
     * <p>
     * To clients, this resource will be available from {@link ScriptBaseClass#resource(String)}.
     */
    @Nullable
    public final File resources;

    /**
     * If non-null, this directory contains Java files shared by service and client.
     *
     */
    @Nullable
    public final File common;


    /**
     * Versions of the program that this test applies.
     */
    @NotNull
    public final VersionProcessor applicableVersions;

    /**
     * Human readable description of this test.
     * This could be long text that spans across multiple lines.
     */
    @Nullable
    public final String description;

    /**
     * Represents a set of transport that this test supports.
     */
    @NotNull
    public final TransportSet supportedTransport;

    /**
     * Bugster IDs that are related to this test.
     * Can be empty set but not null.
     */
    @NotNull
    public final SortedSet<Integer> bugsterIds = new TreeSet<Integer>();

    /**
     * Client test scenarios that are to be executed.
     *
     * <p>
     * When this field is empty, that means the test is just to make sure
     * that the service deploys.
     */
    @NotNull
    public final List<TestClient> clients = new ArrayList<TestClient>();

    /**
     * Optional "set up" script executed before each client script.
     */
    @Nullable
    public final String setUpScript;

    /**
     * Java client.
     */
    @NotNull
    public final List<File> javaClients = new ArrayList<File>();

    /**
     * Services to be deployed for this test.
     */
    @NotNull
    public final List<TestService> services = new ArrayList<TestService>();

    /**
     * Root of the test data directory.
     */
    @NotNull
    public final File home;


    public static final Schema descriptorSchema;

    private boolean skip;

    static {
        URL url = World.class.getResource("test-descriptor.rnc");
        try {
            descriptorSchema = new RelaxNgCompactSyntaxVerifierFactory().compileSchema(url.toExternalForm());
        } catch (SAXParseException e) {
            throw new Error("unable to parse test-descriptor.rnc at line "+e.getLineNumber(),e);
        } catch (Exception e) {
            throw new Error("unable to parse test-descriptor.rnc",e);
        }
    }


    public TestDescriptor(String shortName, File home, File resources, File common,VersionProcessor applicableVersions, String description) {
        this.name = shortName;
        this.home = home;
        this.resources = resources;
        this.common = common;
        this.applicableVersions = applicableVersions;
        this.supportedTransport = TransportSet.ALL;
        this.description = description;
        this.skip=false;
        this.setUpScript = null;
    }

    /**
     * Parses a {@link TestDescriptor} from a test data directory.
     *
     * @param descriptor
     *      Test descriptor XML file.
     */
    public TestDescriptor(File descriptor) throws IOException,DocumentException,ParserConfigurationException,
            SAXException{
        File testDir = descriptor.getParentFile();
        Element root = parse(descriptor).getRootElement();

        VersionProcessor versionProcessor ;
        this.description = root.elementTextTrim("description");
        /*
         * Check if the resources folder exists in the dir where the
         * test-descriptor.xml is present else it is null
         */
        File resourceDir = new File(testDir,"resources");
        this.resources = resourceDir.exists()?resourceDir:null;
        /*
         * Check if the common folder exists in the dir where the
         * test-descriptor.xml is present else it is null
         */
        File commonDir = new File(testDir,"common");
        this.common = commonDir.exists()?commonDir:null;
        this.applicableVersions =  new VersionProcessor(root);

        String skipAttr = root.attributeValue("skip");
        if(skipAttr==null)
            this.skip = false;
        else
            this.skip = new Boolean(skipAttr).booleanValue();

        String transport = root.attributeValue("transport");
        if(transport==null)
            this.supportedTransport = TransportSet.ALL;
        else
            this.supportedTransport = new Singleton(transport);

        String path = testDir.getCanonicalPath();
        String testCasesPattern = "testcases" + File.separatorChar;
        int testCaseIndex = path.lastIndexOf(testCasesPattern);
        testCaseIndex += testCasesPattern.length();
        /*
         * For something like this 'testcases.policy.parsing.someSpecificTest'
         * I think the shortName should be policy.parsing
         * not testcases.policy.parsing as the above would conform to
         * a valid package name too
         */
        this.name = path.substring(testCaseIndex).replace(File.separatorChar,'.');

        this.home = descriptor.getParentFile();

        this.setUpScript = root.elementText("pre-client");

        List<Element> clientList = root.elements("client");
        for (Element client : clientList) {
            versionProcessor = new VersionProcessor(client);

            boolean sideEffectFree = client.attribute("sideEffectFree")!=null;

            if(client.attribute("href")!=null) {
                // reference to script files
                FileSet fs = new FileSet();
                fs.setDir(testDir);
                fs.setIncludes(client.attributeValue("href"));
                for( String relPath : fs.getDirectoryScanner(World.project).getIncludedFiles() ) {
                    TestClient testClient = new TestClient(this,versionProcessor,
                            new Script.File(new File(testDir,relPath)),sideEffectFree);
                    File customization = parseFile(testDir,"custom-client.xml");
                    if (customization.exists() ) {
                        testClient.customizations.add(customization);
                    }
                    File schemaCustomization = parseFile(testDir,"custom-schema-client.xml");
                    if (schemaCustomization.exists() ) {
                        testClient.customizations.add(schemaCustomization);
                    }
                    this.clients.add(testClient);
                }
            } else {
                // literal text
                TestClient testClient = new TestClient(this,versionProcessor,
                    new Script.Inline(client.getText(),client.elementText("description")),
                    sideEffectFree);
                File customization = parseFile(testDir,"custom-client.xml");
                if (customization.exists() ) {
                    testClient.customizations.add(customization);
                }
                File schemaCustomization = parseFile(testDir,"custom-schema-client.xml");
                if (schemaCustomization.exists() ) {
                    testClient.customizations.add(schemaCustomization);
                }
                this.clients.add(testClient);
            }
        }

        findAllJavaClients(home);


        List<Element> serviceList = root.elements("service");
        populateServices(serviceList,testDir,false);
        List<Element> stsList = root.elements("sts");
        populateServices(stsList,testDir,true);
       
    }

    /**
     * Recursively scans the test directory and finds all the Java test files.
     */
    private void findAllJavaClients(File dir) {
        for(File child : dir.listFiles()) {
            if(child.isDirectory())
                findAllJavaClients(child);
            if(child.getName().endsWith("Test.java"))
                javaClients.add(child);
        }
    }


    /**
     * Creates the execution plan of this test descriptor and adds them
     * to {@link TestSuite} (so that when {@link TestSuite}
     * is executed, you execute this test.
     *
     * @param container The container to host the services.
     * @param concurrentSideEffectFree
     *      See {@link Main#concurrentSideEffectFree}
     * @return
     *      {@link TestSuite} that contains test execution plan for this test.
     */
    public TestSuite build(ApplicationContainer container, WsTool wsimport, boolean concurrentSideEffectFree) throws IOException {

        TestSuite suite = new TestSuite();

        if (skip) {
            System.out.println("Skipping "+name+"; explictly marked to skip.");
            return suite;
        }

        if(!supportedTransport.contains(container.getTransport())) {
            System.out.println("Skipping "+name+" as it's not applicable to "+container.getTransport());
            return suite;
        }

        DeploymentContext context = new DeploymentContext(this,container,wsimport);

        List<DeploymentExecutor> deployTests = new ArrayList<DeploymentExecutor>();

        // first prepare the working directories.
        // we shouldn't do it after the run, or else developers won't be able to
        // see what's generated to debug problems
        // in the -skip mode, don't clean
        suite.addTest(new PrepareExecutor(context, !wsimport.isNoop()));

        // deploy all services
        for (DeployedService s : context.services.values()) {
            DeploymentExecutor dt = new DeploymentExecutor(s);
            deployTests.add(dt);
            suite.addTest(dt);
        }

        // run client test scripts
        for (TestClient c : clients) {
            if(concurrentSideEffectFree && c.sideEffectFree) {
                suite.addTest(new ConcurrentClientExecutor.Fixed(context,c));
                suite.addTest(new ConcurrentClientExecutor.Cached(context,c));
            } else
                suite.addTest(new ClientExecutor(context, c));
        }

        // run client Java tests
        for (File f : javaClients)
            suite.addTest(new JavaClientExecutor(context,f));

        // undeploy all services
        for (DeploymentExecutor dt : deployTests) {
            suite.addTest(dt.createUndeployer());
        }

        return suite;
    }

    /**
     * Parses a potentially relative file path.
     */
    private File parseFile(File base, String href) {

        File f = new File(href);
        if (f.isAbsolute())
            return f;
        else
            return new File(base, href);
    }

    /**
     * Parses a test descriptor.
     */
    private Document parse(File descriptor) throws DocumentException, SAXException, ParserConfigurationException {
        SAXParserFactory factory;
        if (descriptorSchema != null) {
            factory = ValidatingSAXParserFactory.newInstance(descriptorSchema);
        } else {
            factory = SAXParserFactory.newInstance();
        }
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        return new SAXReader(factory.newSAXParser().getXMLReader()).read(descriptor);
    }

    /**
     * Returns a human readable name that identifies the test,
     * for better readability of the test result report.
     */
    public String toString() {
        return name;
    }

    class XSDFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".xsd"));
        }
    }

    /**
     * This filter gives all wsdls in the directory excluding primaryWsdl.
     * This can be used to gather all imported wsdls.
     */

    class WSDLFilter implements FilenameFilter {
        String primaryWsdl;
        public WSDLFilter(String primaryWsdl){
            this.primaryWsdl = primaryWsdl;
        }
        public boolean accept(File dir, String name) {
            return (name.endsWith(".wsdl") && (!name.equals(primaryWsdl)));
        }
    }

    private void populateServices(List<Element> serviceList, File testDir, boolean isSTS) throws IOException {
        for (Element service : serviceList) {
            String baseDir = service.attributeValue("basedir", ".");

            String serviceName;
            File serviceBaseDir;
            if (!baseDir.equals(".")) {
                serviceBaseDir = new File(testDir, baseDir);
                serviceName = serviceBaseDir.getCanonicalFile().getName();
            } else {
                serviceName = "";
                serviceBaseDir = testDir;
            }

            File wsdl;
            WSDL wsdlInfo = null;
            if (service.element("wsdl") != null) {
                String wsdlAttribute = service.element("wsdl").attributeValue("href", "test.wsdl");
                wsdl = parseFile(serviceBaseDir, wsdlAttribute);
                File[] schemas = serviceBaseDir.listFiles(new XSDFilter());
                File[] wsdls = serviceBaseDir.listFiles(new WSDLFilter(wsdlAttribute));
                List<File> importedWsdls = Arrays.asList(wsdls);
                List<File> schemaFiles = Arrays.asList(schemas);
                wsdlInfo = new WSDL(wsdl, importedWsdls, schemaFiles);

            }

            TestService testService = new TestService(this, serviceName, serviceBaseDir, wsdlInfo, isSTS,
                service.attributeValue("class"));
            File customization = parseFile(serviceBaseDir, "custom-server.xml");
            if (customization.exists()) {
                testService.customizations.add(customization);
            }
            File schemaCustomization = parseFile(serviceBaseDir, "custom-schema-server.xml");
            if (schemaCustomization.exists()) {
                testService.customizations.add(schemaCustomization);
            }


            this.services.add(testService);

        }
    }
}
