package com.sun.xml.ws.test;

import com.sun.istack.test.AntXmlFormatter;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.cargo.EmbeddedCargoApplicationContainer;
import com.sun.xml.ws.test.container.cargo.InstalledCargoApplicationContainer;
import com.sun.xml.ws.test.container.cargo.RemoteCargoApplicationContainer;
import com.sun.xml.ws.test.container.gf.GlassfishContainer;
import com.sun.xml.ws.test.container.local.LocalApplicationContainer;
import com.sun.xml.ws.test.model.TestDescriptor;
import com.sun.xml.ws.test.tool.WsTool;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.codehaus.classworlds.ClassWorld;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Option(name="-external-wsimport",usage="use external tool.sh/.bat")
    File externalWsImport = null;

    @Option(name="-external-wsgen",usage="use external wsgen.sh/.bat")
    File externalWsGen = null;

    @Option(name="-skip",usage="skip all code generation and reuse the artifacts generated during the last run")
    boolean skipCompilation;

    /**
     * This is copied to {@link World#debug}.
     */
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
    @Option(name="-tomcat-local",usage="Launch Tomcat from the harness and test with it",metaVar="TOMCAT_HOME")
    File tomcat = null;

    @Option(name="-tomcat-remote",metaVar="CONFIG",
        usage="Test with remote Tomcat.\n" +
            "CONFIG=[USER:PASS@]HOST[:PORT].\n" +
            "Defaults: USER=admin, PASS=admin, PORT=8080")
    String remoteTomcat = null;

    @Option(name="-tomcat-embedded",metaVar="TOMCAT_HOME",
        usage="loads Tomcat into the harness VM and test with it.")
    File embeddedTomcat = null;

    @Option(name="-glassfish-remote",metaVar="CONFIG",
        usage=
            "Test with remote Glassfish. Needs both JMX connection info and HTTP URL.\n"+
            "CONFIG=[USER:PASS@]HOST[:PORT][-HTTPURL]\n"+
            "e.g., admin:adminadmin@localhost:4848-http://localhost:8080/\n"+
            "Defaults: USER=admin, PASS=adminadmin, PORT=4848, HTTPURL=http://HOST/")
    String remoteGlassfish = null;


    @Option(name="-report",usage="Generate JUnit test report XMLs",metaVar="DIR")
    File reportDir = null;


    public static void main(String[] args) throws Exception {
        // enable all assertions
        Main.class.getClassLoader().setDefaultAssertionStatus(true);
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
        WsTool wsimport,wsgen;
        if(skipCompilation) {
            System.err.println("Skipping compilation");
            wsimport = wsgen = WsTool.NOOP;
        } else {
            wsimport = WsTool.createWsImport(externalWsImport);
            wsgen = WsTool.createWsGen(externalWsGen);
        }
        ApplicationContainer container = createContainer(wsimport,wsgen);

        // build up test plan
        TestSuite suite = createTestSuite();
        for (String dir : tests)
            build(new File(dir), container, wsimport, suite);

        if(suite.countTestCases()==0) {
            System.err.println("No test to run");
            return -1;
        }

        // run the tests
        try {
            container.start();

            // custom TestRunner that can generate Ant format report
            TestRunner testRunner = new TestRunner() {
                private AntXmlFormatter formatter;

                protected TestResult createTestResult() {
                    TestResult result = super.createTestResult();

                    if(reportDir!=null) {
                        reportDir.mkdirs();
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
    private void fillWorld() throws Exception {
        World.debug = this.debug;

        Realm runtime = World.runtime;
        Realm tool = World.tool;

        // fill in container realm.
        if(embeddedTomcat!=null) {
            runtime.addJarFolder(new File(embeddedTomcat,"bin"));
            runtime.addJarFolder(new File(embeddedTomcat,"common/lib"));
            runtime.addJarFolder(new File(embeddedTomcat,"server/lib"));
        }

        // fill in runtime and tool realms
        if(wsitImage!=null) {
            runtime.addJar(new File(wsitImage,"lib/webservices.jar"));
            runtime.addJar(   new File(wsitImage,"lib/webservices-tools.jar"));
            tool.addJar(   new File(wsitImage,"lib/webservices-tools.jar"));
            //tool.addJar(new File(wsitImage,"lib/webservices.jar"));
        } else
        if(wsitWs!=null) {
            runtime.addClassFolder( new File(wsitWs,"rt/build/classes"));
            runtime.addJarFolder(   new File(wsitWs,"lib/runtime"));
            /*runtime.addClassFolder(    new File(wsitWs,"tools/build/classes"));
            runtime.addJarFolder(      new File(wsitWs,"lib/tooltime"));*/
            tool.addClassFolder(    new File(wsitWs,"tools/build/classes"));
            tool.addJarFolder(      new File(wsitWs,"lib/tooltime"));
        } else
        if(jaxwsImage!=null) {
            tool.addJar(            new File(jaxwsImage,"lib/jaxws-tools.jar"));
            tool.addJar(            new File(jaxwsImage,"lib/jaxb-xjc.jar"));
            runtime.addJarFolder(   new File(jaxwsImage,"lib"), "jaxws-tools.jar","jaxb-xjc.jar");
        } else
        if(jaxwsWs!=null) {
            runtime.addClassFolder( new File(jaxwsWs,"rt/build/classes"));
            runtime.addClassFolder( new File(jaxwsWs,"rt/src"));
            runtime.addClassFolder( new File(jaxwsWs,"servlet/build/classes"));
            runtime.addClassFolder( new File(jaxwsWs,"servlet/src"));
            runtime.addClassFolder( new File(jaxwsWs,"rt-fi/build/classes"));
            runtime.addClassFolder( new File(jaxwsWs,"transports/local/build/classes"));
            runtime.addClassFolder( new File(jaxwsWs,"transports/local/src"));
            tool.addClassFolder(    new File(jaxwsWs,"tools/wscompile/build/classes"));
            // this is needed for Localizer (which lives in runtime) to find message resources of wsimport
            runtime.addClassFolder(    new File(jaxwsWs,"tools/wscompile/src"));
            tool.addJar(            new File(jaxwsWs,"lib/jaxb-xjc.jar"));
            runtime.addJarFolder(   new File(jaxwsWs,"lib"),    "jaxb-xjc.jar");
        } else {
            // TODO: if none is given, wouldn't it be nice if we can guess?
            // TODO: don't we need a better way to discover local transport.

            throw new CmdLineException("No -cp option is specified");
        }

        // put tools.jar in the tools classpath
        File jreHome = new File(System.getProperty("java.home"));
        File toolsJar = new File( jreHome.getParent(), "lib/tools.jar" );
        tool.addJar(toolsJar);
        //TODO remove me temporary workaround
        runtime.addJar(toolsJar);

        if(debug) {
            System.err.println("runtime realm");
            runtime.dump(System.err);
            System.err.println("tool realm");
            tool.dump(System.err);
        }

    }

    /**
     * Determines the container to be used for tests.
     * @param wsimport
     */
    private ApplicationContainer createContainer(WsTool wsimport, WsTool wsgen) throws Exception {
        if(tomcat!=null) {
            System.err.println("Using Tomcat from "+tomcat);
            return new InstalledCargoApplicationContainer(
                wsimport, wsgen, "tomcat5x",tomcat);
        }

        if(embeddedTomcat!=null) {
            return new EmbeddedCargoApplicationContainer(
                wsimport, wsgen, "tomcat5x");
        }

        if(remoteTomcat!=null) {
            System.err.println("Using remote Tomcat at "+remoteTomcat);
            //  group capture number  :        12    3      4      5 6
            Matcher matcher = Pattern.compile("((.+):(.*)@)?([^:]+)(:([0-9]+))?").matcher(remoteTomcat);
            if(!matcher.matches())
                throw new CmdLineException("Unable to parse "+remoteTomcat);

            return new RemoteCargoApplicationContainer(
                wsimport, wsgen,
                "tomcat5x",
                new URL("http",matcher.group(4),
                    Integer.parseInt(defaultsTo(matcher.group(6),"8080")),
                    "/"),
                defaultsTo(matcher.group(2),"admin"),
                defaultsTo(matcher.group(3),"admin")
                );
        }

        if(remoteGlassfish!=null) {
            // [USER:PASS@]HOST[:PORT][-HTTPURL]
            System.err.println("Using remote Glassfish at "+remoteGlassfish);
            //  group capture number  :        12    3      4         5 6         7   8
            Matcher matcher = Pattern.compile("((.+):(.*)@)?([^:\\-]+)(:([0-9]+))?(\\-(.+))?").matcher(remoteGlassfish);
            if(!matcher.matches())
                throw new CmdLineException("Unable to parse "+remoteGlassfish);

            String userName = defaultsTo(matcher.group(2),"admin");
            String password = defaultsTo(matcher.group(3),"adminadmin");
            String host = matcher.group(4);
            String port = defaultsTo(matcher.group(6),"4848");
            String httpUrl = matcher.group(8);

            if(httpUrl==null) {
                // defaulted
                httpUrl = "http://"+host+":8080/";
            }

            return new GlassfishContainer(
                wsimport, wsgen, new URL(httpUrl), host, Integer.parseInt(port), userName, password
            );
        }


        System.err.println("Testing with the local transport");
        return new LocalApplicationContainer(wsimport,wsgen);
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
            suite.addTest(new TestDescriptor(descriptor).build(container,wsimport));
            return;
        }

        if(recursive && dir.isDirectory()) {
            // find test data recursively
            File[] subdirs = dir.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });

            for (File subdir : subdirs)
                build(subdir,container, wsimport, suite);
        }
    }
}
