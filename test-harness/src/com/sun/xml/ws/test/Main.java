package com.sun.xml.ws.test;

import com.sun.istack.test.AntXmlFormatter;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.cargo.InstalledCargoApplicationContainer;
import com.sun.xml.ws.test.container.cargo.RemoteCargoApplicationContainer;
import com.sun.xml.ws.test.container.local.LocalApplicationContainer;
import com.sun.xml.ws.test.model.TestDescriptor;
import com.sun.xml.ws.test.wsimport.WsTool;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.framework.Test;
import junit.textui.TestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.NoSuchRealmException;
import org.dom4j.DocumentException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.junit.ParallelTestSuite;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;

/**
 * Test harness driver.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    // private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static final ClassWorld world = new ClassWorld();

    /**
     * Tests to be executed.
     */
    @Argument
    final List<String> tests = new ArrayList<String>();

    @Option(name="-r",usage="find test directories recursively")
    boolean recursive = false;

    @Option(name="-p",usage="run multiple tests in parallel")
    int parallel = 1;

    /*
      Classpath builder variables
      ---------------------------

      Test harness needs to load various JAX-WS/Tango classes to do its work,
      yet where those classes are placed depends what the user is working on.

      Harness defines a few 'profiles' of the jar file layout, and allow users
      to choose them.
    */
    @Option(name="-cp:wsit-image",usage="classpath option\npath to the WSIT dist image",metaVar="WSIT_HOME")
    File wsitImage = null;

    @Option(name="-cp:wsit",usage="classpath option\npath to WSIT workspace",metaVar="WSIT_HOME")
    File wsitWs = null;

    @Option(name="-cp:jaxws-image",usage="classpath option\npath to JAX-WS RI dist image",metaVar="JAXWS_HOME")
    File jaxwsImage = null;

    @Option(name="-cp:jaxws",usage="classpath option\npath to JAX-WS RI workspace",metaVar="JAXWS_HOME")
    File jaxwsWs = null;

    @Option(name="-external-wsimport",usage="use external wsimport.sh/.bat")
    File externalWsImport = null;

    @Option(name="-skip",usage="skip all code generation and reuse the artifacts generated during the last run")
    boolean skipCompilation;

    @Option(name="-debug",usage="Generate output for debugging harness")
    boolean debug;

    /*
      Container variables
      -------------------

      Options that choose the container to host services.
      They are mutually exclusive, but we don't have means to enforce
      exclusiveness right now.

      If none is given we test in the local mode.
    */
    @Option(name="-tomcat",usage="Test with embedded Tomcat",metaVar="TOMCAT_HOME")
    File tomcat = null;

    @Option(name="-remote-tomcat",metaVar="[USER:PASS@]HOST[:PORT]",
        usage="Test with remote Tomcat\nNeeds login info for admin. Defaults: USER=admin, PASS=admin, PORT=8080")
    String remoteTomcat = null;

    @Option(name="-glassfish",usage="Test with Glassfish",metaVar="GLASSFISH_HOME")
    File glassfish = null;

    @Option(name="-remote-glassfish",usage="Test with remote Glassfish",metaVar="HOST:PORT")
    String remoteGlassfish = null;


    @Option(name="-report",usage="Generate JUnit test report XMLs",metaVar="DIR")
    File reportDir = null;


    public static void main(String[] args) throws Exception {
        System.exit(doMain(args));
    }

    public static int doMain(String[] args) throws Exception {
        Main main = new Main();
        CmdLineParser parser = new CmdLineParser(main);
        try {
            parser.parseArgument(args);

            if(main.tests.isEmpty())
                throw new CmdLineException("No test is given");

            return main.run();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return -1;
        }
    }

    /**
     * Heart of the test.
     */
    public int run() throws Exception {
        fillWorld();

        // set up objects that represent test environment.
        WsTool wsimport;
        if(skipCompilation) {
            System.err.println("Skipping compilation");
            wsimport = WsTool.NOOP;
        } else {
            wsimport = WsTool.createWsImport(externalWsImport);
        }
        ApplicationContainer container = createContainer(wsimport);

        // build up test plan
        TestSuite suite = createTestSuite();
        for (String dir : tests)
            build(new File(dir), container, wsimport, suite);

        if(suite.countTestCases()==0) {
            System.err.println("No test to run");
            return -1;
        }

        // needs to be created before test starts to run,

        // run the tests
        try {
            container.start();

            // custom TestRunner that can generate Ant format report
            TestRunner testRunner = new TestRunner() {
                private AntXmlFormatter formatter;

                protected TestResult createTestResult() {
                    TestResult result = super.createTestResult();

                    if(reportDir!=null) {
                        formatter = new AntXmlFormatter(XMLJUnitResultFormatter.class, reportDir);
                        result.addListener(formatter);
                    }
                    return result;
                }

                public TestResult doRun(Test test) {
                    try {
                        return super.doRun(test);
                    } finally {
                        if(formatter!=null)
                            formatter.close();
                    }
                }
            };

            TestResult r = testRunner.doRun(suite);

            return r.errorCount()+r.failureCount();
        } finally {
            container.shutdown();
        }
    }

    /**
     * Fills the world with classes.
     */
    private void fillWorld() throws IOException {
        RealmBuilder runtime = new RealmBuilder(World.runtime,World.runtimeClasspath);
        RealmBuilder tool = new RealmBuilder(World.tool,World.toolClasspath);

        if(wsitImage!=null) {
            runtime.addJar(new File(wsitImage,"lib/webservices.jar"));
            tool.addJar(   new File(wsitImage,"lib/webservices-tools.jar"));
        }
        if(wsitWs!=null) {
            runtime.addClassFolder( new File(wsitWs,"rt/build/classes"));
            runtime.addJarFolder(   new File(wsitWs,"lib/runtime"));
            tool.addClassFolder(    new File(wsitWs,"tools/build/classes"));
            tool.addJarFolder(      new File(wsitWs,"lib/tooltime"));
        }
        if(jaxwsImage!=null) {
            tool.addJar(            new File(jaxwsWs,"lib/jaxws-tools.jar"));
            tool.addJar(            new File(jaxwsWs,"lib/jaxb-xjc.jar"));
            runtime.addJarFolder(   new File(jaxwsImage,"lib"), "jaxws-tools.jar","jaxb-xjc.jar");
        }
        if(jaxwsWs!=null) {
            runtime.addClassFolder( new File(jaxwsWs,"rt/build/classes"));
            runtime.addClassFolder( new File(jaxwsWs,"rt-fi/build/classes"));
            runtime.addClassFolder( new File(jaxwsWs,"transports/local/build/classes"));
            runtime.addClassFolder( new File(jaxwsWs,"transports/local/src"));
            tool.addClassFolder(    new File(jaxwsWs,"tools/wscompile/build/classes"));
            tool.addJar(            new File(jaxwsWs,"lib/jaxb-xjc.jar"));
            runtime.addJarFolder(   new File(jaxwsWs,"lib"),    "jaxb-xjc.jar");
        }

        // put tools.jar in the tools classpath
        File jreHome = new File(System.getProperty("java.home"));
        File toolsJar = new File( jreHome.getParent(), "lib/tools.jar" );
        tool.addJar(toolsJar);
        
        World.runtimeClasspath = runtime.getClasspath();
        World.toolClasspath = tool.getClasspath();

        if(debug) {
            System.err.println("runtime realm");
            runtime.dump(System.err);
            System.err.println("tool realm");
            tool.dump(System.err);
        }

        // jaxb-xjc.jar will suck in JAXB runtime and API through Class-Path manifest entries
        // into the tool realm, so we'll end up loading two JAXB API.
        // avoid this by importing API from runtime. This isn't the best fix, however.
        try {
            World.tool.importFrom("runtime","javax.xml.bind");
        } catch (NoSuchRealmException e) {
            throw new AssertionError(e);
        }

        // TODO: if none is given, wouldn't it be nice if we can guess?
        // TODO: don't we need a better way to discover local transport.
    }

    /**
     * Determines the container to be used for tests.
     * @param wsimport
     */
    private ApplicationContainer createContainer(WsTool wsimport) throws Exception {
        if(tomcat!=null) {
            System.err.println("Using Tomcat from "+tomcat);
            return new InstalledCargoApplicationContainer("tomcat5x",tomcat);
        }

        if(remoteTomcat!=null) {
            System.err.println("Using remote Tomcat at "+remoteTomcat);
            //  group capture number  :        12    3      4   5 6
            Matcher matcher = Pattern.compile("((.+):(.*)@)?(.+)(:([0-9]+))?").matcher(remoteTomcat);
            if(!matcher.matches())
                throw new CmdLineException("Unable to parse "+remoteTomcat);

            return new RemoteCargoApplicationContainer("tomcat5x",
                new URL("http",matcher.group(4),
                    Integer.parseInt(defaultsTo(matcher.group(6),"8080")),
                    "/"),
                defaultsTo(matcher.group(2),"admin"),
                defaultsTo(matcher.group(3),"admin")
                );
        }

        if(glassfish!=null)
            // TODO: implement this later
            throw new UnsupportedOperationException();

        if(remoteGlassfish!=null)
            // TODO: implement this later
            throw new UnsupportedOperationException();


        System.err.println("Testing with the local transport");
        return new LocalApplicationContainer(wsimport);
    }

    private static String defaultsTo( String value, String defaultValue ) {
        if(value==null)     return defaultValue;
        else                return value;
    }

    /**
     * Creates {@link TestSuite} that hides sequential/parallel execution
     * of tests.
     */
    private TestSuite createTestSuite() {
        if (parallel>1) {
            System.err.println("Running tests in "+parallel+" threads");
            return new ParallelTestSuite();
        } else {
            return new TestSuite();
        }
    }

    /**
     * Scans the given directory, builds {@link TestDescriptor}s,
     * and schedule them to {@link TestSuite}.
     */
    private void build(File dir, ApplicationContainer container, WsTool wsimport, TestSuite suite) throws IOException, DocumentException, ParserConfigurationException,
            SAXException {
        File descriptor = new File(dir,"test-descriptor.xml");

        if(descriptor.exists()) {
            new TestDescriptor(descriptor).build(container,wsimport,suite);
            return;
        }

        if(recursive && descriptor.isDirectory()) {
            // find test data recursively
            File[] subdirs = descriptor.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });

            for (File subdir : subdirs)
                build(subdir,container, wsimport, suite);
        }
    }
}
