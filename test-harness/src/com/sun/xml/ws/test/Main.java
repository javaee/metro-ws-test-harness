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

package com.sun.xml.ws.test;

import bsh.Interpreter;
import com.sun.istack.test.AntXmlFormatter;
import com.sun.istack.test.VersionNumber;
import com.sun.xml.ws.test.container.ApplicationContainer;
import com.sun.xml.ws.test.container.cargo.EmbeddedCargoApplicationContainer;
import com.sun.xml.ws.test.container.cargo.InstalledCargoApplicationContainer;
import com.sun.xml.ws.test.container.cargo.RemoteCargoApplicationContainer;
import com.sun.xml.ws.test.container.gf.GlassfishContainer;
import com.sun.xml.ws.test.container.invm.InVmContainer;
import com.sun.xml.ws.test.container.javase.JavaSeContainer;
import com.sun.xml.ws.test.container.local.LocalApplicationContainer;
import com.sun.xml.ws.test.emma.Emma;
import com.sun.xml.ws.test.model.TestDescriptor;
import com.sun.xml.ws.test.tool.WsTool;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
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

    @Option(name="-cp:metrov3-image",usage="classpath option\npath to the Metro for GlassFish v3 workspace",metaVar="WSIT_HOME")
    File metroV3Image = null;

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

    @Option(name="-transport",usage="specify the pluggable transport jar")
    File transportJar;

    @Option(name="-cp:override",usage="these jars and folders are placed in front of other -cp:*** options. Useful for overriding some jars")
    String classPathOverride;

    /**
     * This is copied to {@link World#debug}.
     */
    @Option(name="-debug",usage="Generate output for debugging harness")
    boolean debug;

    @Option(name="-dump",usage="Enable all transport dumps")
    boolean dump;

    @Option(name="-report",usage="Generate JUnit test report XMLs",metaVar="DIR")
    File reportDir = null;

    @Option(name="-concurrent-side-effect-free",usage="Run all side-effect free tests as concurrent")
    boolean concurrentSideEffectFree = false;

    @Option(name="-emma",usage="Generate emma coverage report")
    File emma = null;

    @Option(name="-version",usage="Specify the target JAX-WS version being tested. This determines test exclusions",handler=VersionNumberHandler.class)
    VersionNumber version = null;

    @Option(name="-client",usage="Just run a single client script, instead of all")
    String clientScriptName = null;

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


    @Option(name="-jetty-embedded",metaVar="JETTY_HOME",
        usage="loads Jetty into the harness VM and test with it.")
    File embeddedJetty;

    @Option(name="-lwhs",usage="tests using the Java lightweight HTTP server")
    boolean lwhs = false;

    @Option(name="-jaxwsInJDK",usage="tests using JAX-WS impl in JDK")
    boolean jaxwsInJDK = false;

    
    @Option(name="-glassfish-remote",metaVar="CONFIG",
        usage=
            "Test with remote Glassfish. Needs both JMX connection info and HTTP URL.\n"+
            "CONFIG=[USER:PASS@]HOST[:PORT][-HTTPURL]\n"+
            "e.g., admin:adminadmin@localhost:4848-http://localhost:8080/\n"+
            "Defaults: USER=admin, PASS=adminadmin, PORT=4848, HTTPURL=http://HOST/")
    String remoteGlassfish = null;

    @Option(name="-glassfish-local",metaVar="GLASSFISH_HOME",
        usage=
            "Launch Glassfish from the harness and test with it")
    File localGlassfish = null;

    @Option(name="-legacy-local",usage="Emergency! I need to use the legacy local transport!")
    boolean legacyLocalTransport = false;

    @Option(name="-leave",usage="leave the container running after all the tests are completed. Often useful for debugging problems.")
    boolean leave = false;

    @Option(name="-port",usage="Choose the TCP port used for local/embedded container-based tests. Set to -1 to choose random port.")
    int port = 18080;

    @Option(name="-wsgen",usage=
            "Control the packaging of Wrapper and Exception beans.\n"+
            "always - Beans are packaged.\n"+
            "both - Test case is executed twice, with and without packaging beans.\n"+
            "ignore - Beans are NOT packaged.", metaVar="[always|both|ignore]"
    )
    WsGenMode wsGenMode = WsGenMode.ALWAYS;

    public static File[] containerClasspathPrefix;

    public static void main(String[] args) throws Exception {
        // enable all assertions
        Main.class.getClassLoader().setDefaultAssertionStatus(true);

        // use the platform default proxy if available.
        // see sun.net.spi.DefaultProxySelector for details.
        try {
            System.setProperty("java.net.useSystemProxies","true");
        } catch (SecurityException e) {
            // failing to set this property isn't fatal
        }
        
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

        if(dump) {
            System.setProperty("com.sun.xml.ws.transport.local.LocalTransportTube.dump","true");
            System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump","true");
            System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump","true");
        }


        if(port==-1) {
            // set TCP port to somewhere between 20000-60000
            port = new Random().nextInt(40000) + 20000;
        }

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

        container.start();

        try {// run the tests

            // custom TestRunner that can generate Ant format report
            TestRunner testRunner = new TestRunner() {
                private AntXmlFormatter formatter;

                @Override
                protected TestResult createTestResult() {
                    TestResult result = super.createTestResult();

                    if(reportDir!=null) {
                        reportDir.mkdirs();
                        formatter = new AntXmlFormatter(XMLJUnitResultFormatter.class, reportDir);
                        result.addListener(formatter);
                    }
                    return result;
                }

                @Override
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
            if(!leave)
                container.shutdown();
            if(World.emma!=null)
                World.emma.write(emma);
        }
    }

    /**
     * Fills the world with classes.
     */
    private void fillWorld() throws Exception {
        World.debug = this.debug;
        World.emma = emma!=null ? new Emma() : null;

        Realm runtime = World.runtime;
        Realm tool = World.tool;

        if(classPathOverride!=null) {
            StringTokenizer tokens = new StringTokenizer(classPathOverride,File.pathSeparator);
            while(tokens.hasMoreTokens()) {
                runtime.addJar(new File(tokens.nextToken()));
            }
        }

        if(transportJar!=null)
            runtime.addJar(transportJar);

        // fill in container realm.
        if(embeddedTomcat!=null) {
            runtime.addJarFolder(new File(embeddedTomcat,"bin"));
            runtime.addJarFolder(new File(embeddedTomcat,"common/lib"));
            runtime.addJarFolder(new File(embeddedTomcat,"server/lib"));
        }

        if(embeddedJetty!=null) {
            runtime.addJarFolder(embeddedJetty);
            runtime.addJarFolder(new File(embeddedJetty,"lib"));
        }

        if(wsitImage==null && wsitWs==null && metroV3Image == null && jaxwsImage==null && jaxwsWs==null && !jaxwsInJDK)
            guessWorkspace();

        // fill in runtime and tool realms
        if(wsitImage!=null) {
            File rtJar = new File(wsitImage,"lib/webservices-rt.jar");
            runtime.addJar(rtJar);
            
            File toolJar = new File(wsitImage,"lib/webservices-tools.jar");
            tool.addJar(toolJar);
            
            File apiJar = new File(wsitImage,"lib/webservices-api.jar");
            runtime.addJar(apiJar);

            File extraJar = new File(wsitImage,"lib/webservices-extra.jar");
            runtime.addJar(extraJar);

            File extraApiJar = new File(wsitImage,"lib/webservices-extra-api.jar");
            runtime.addJar(extraApiJar);

            containerClasspathPrefix = new File[5];
            containerClasspathPrefix[0] = rtJar;
            containerClasspathPrefix[1] = toolJar;
            containerClasspathPrefix[2] = apiJar;
            containerClasspathPrefix[3] = extraJar;
            containerClasspathPrefix[4] = extraApiJar;
        } else
        if(wsitWs!=null) {
            runtime.addClassFolder( new File(wsitWs,"rt/build/classes"));
            runtime.addJarFolder(   new File(wsitWs,"lib/runtime"));
            /*runtime.addClassFolder(    new File(wsitWs,"tools/build/classes"));
            runtime.addJarFolder(      new File(wsitWs,"lib/tooltime"));*/
            tool.addClassFolder(    new File(wsitWs,"tools/build/classes"));
            tool.addJarFolder(      new File(wsitWs,"lib/tooltime"));
        } else
        if(metroV3Image!=null) {
            File apiJar = new File(metroV3Image,"webservices-api.jar");
            runtime.addJar(apiJar);

            File rtJar = new File(metroV3Image,"webservices-rt.jar");
            runtime.addJar(rtJar);
            
            File toolJar = new File(metroV3Image,"webservices-tools.jar");
            tool.addJar(toolJar);
            
            containerClasspathPrefix = new File[3];
            containerClasspathPrefix[0] = apiJar;
            containerClasspathPrefix[1] = rtJar;
            containerClasspathPrefix[2] = toolJar;
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
        } else if (jaxwsInJDK) {
            System.out.println("Using JAX-WS in JDK");
            File jreHome = new File(System.getProperty("java.home"));
            externalWsGen = new File( jreHome.getParent(), "bin/wsgen" );
            if (!externalWsGen.exists()) {
                externalWsGen = new File( jreHome.getParent(), "bin/wsgen.exe" );
            }
            externalWsImport = new File( jreHome.getParent(), "bin/wsimport" );
            if (!externalWsImport.exists()) {
                externalWsImport = new File( jreHome.getParent(), "bin/wsimport.exe" );
            }
            if (!externalWsGen.exists() || !externalWsImport.exists()) {
                throw new CmdLineException("wsgen or wsimport command line tools are not found in jdk");
            }
            System.out.println("Using wsgen from "+externalWsGen);
            System.out.println("Using wsimport from "+externalWsImport);
            lwhs = true;
            if (version == null) {
                version = new VersionNumber("2.1.1");
            }
            System.out.println("Going to use SE lightweight http server");
        } else {
            throw new CmdLineException("No -cp option is specified, nor were we able to guess the -cp option");
        }

        // pick up ${HARNESS_HOME}/ext jars
        String harnessHome = System.getProperty("HARNESS_HOME");
        if(harnessHome!=null) {
            File extDir = new File(new File(harnessHome),"ext");
            if(debug)
                System.err.println("Searching extensions in "+extDir);
            if(extDir.exists()) {
                for( File f : extDir.listFiles(new FileFilter() {
                    public boolean accept(File f) {
                        return f.getName().endsWith(".jar");
                    }
                })) {
                    System.err.println("Picking up extension: "+f);
                    runtime.addJar(f);
                }
            }
        } else {
            System.err.println("No extension jar");
        }


        // put tools.jar in the tools classpath
        File jreHome = new File(System.getProperty("java.home"));
        File toolsJar = new File( jreHome.getParent(), "lib/tools.jar" );
        if (toolsJar.exists())
            tool.addJar(toolsJar);
        // For Mac OS X
        File classesJar = new File( jreHome.getParent(), "Classes/classes.jar" );
        if (classesJar.exists())
            tool.addJar(classesJar);


        if(debug) {
            Interpreter.DEBUG = true;
            System.err.println("runtime realm");
            runtime.dump(System.err);
            System.err.println("tool realm");
            tool.dump(System.err);

            // install listener to Ant project so that we can get logging from there
            DefaultLogger listener = new DefaultLogger();
            World.project.addBuildListener(listener);
            listener.setMessageOutputLevel(Project.MSG_INFO);
            listener.setOutputPrintStream(System.out);
            listener.setErrorPrintStream(System.err);
        }

    }

    /**
     * Guess which workspace we want to test against, in case no "-cp" is given.
     */
    private void guessWorkspace() {
        // JAX-WS RI teams often set this variable
        String jaxwsHome = System.getenv("JAXWS_HOME");
        if(jaxwsHome!=null) {
            File f = new File(jaxwsHome);
            if(f.isDirectory()) {
                if(f.getName().equals("build")) {
                    // probably being set to jaxws-ri/build. Let's verify.
                    File home = f.getParentFile();
                    if(new File(home,".jaxws-ri").exists()) {
                        System.out.println("Found JAX-WS RI workspace at "+home);
                        jaxwsWs = home;
                        return;
                    }
                }

                // is this really JAX-WS home?
                if(new File(f,".jaxws-ri").exists()) {
                    System.out.println("Found JAX-WS RI workspace at "+f);
                    jaxwsWs = f;
                    return;
                }

                // the other possibility is it's pointing to the JAX-WS RI distribution image
                if(new File(f,"lib/jaxws-rt.jar").exists() && new File(f,"bin/wsgen.bat").exists()) {
                    System.out.println("Found JAX-WS RI distribution image at "+f);
                    jaxwsImage = f;
                    return;
                }
            }
        }

        // let's go up the directory hierarchy a bit to find a match
        File harnessJar = getHarnessJarDirectory();
        File jaxwsUnit = getParentWithName(harnessJar,"jaxws-unit");
        if(jaxwsUnit!=null) {
            for( File other : jaxwsUnit.getParentFile().listFiles(DIRECTORY_FILTER)) {
                if(new File(other,".jaxws-ri").exists()) {
                    System.out.println("Found JAX-WS RI workspace at "+other);
                    jaxwsWs = other;
                    return;
                }
            }
        }

        // are we in WSIT?
        File wsitHome = getParentWithFile(harnessJar,".wsit");
        if(wsitHome!=null) {
            System.out.println("Found WSIT workspace at "+wsitHome);
            this.wsitWs = wsitHome;
            return;
        }

        // couldn't make any guess
    }

    /**
     * Determines the container to be used for tests.
     * @param wsimport
     */
    private ApplicationContainer createContainer(WsTool wsimport, WsTool wsgen) throws Exception {
        if(tomcat!=null) {
            System.err.println("Using Tomcat from "+tomcat);
            return new InstalledCargoApplicationContainer(
                wsimport, wsgen, "tomcat5x",tomcat,port);
        }

        if(embeddedTomcat!=null) {
            return new EmbeddedCargoApplicationContainer(
                wsimport, wsgen, "tomcat5x",port);
        }

        if(embeddedJetty!=null) {
            return new EmbeddedCargoApplicationContainer(
                wsimport, wsgen, "jetty6x",port);
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

        if(localGlassfish!=null) {
            System.err.println("Using local Glassfish from "+localGlassfish);
            return new InstalledCargoApplicationContainer(
                wsimport, wsgen, "glassfish1x",localGlassfish,port);
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
            String remoteHost = matcher.group(4);
            String remotePort = defaultsTo(matcher.group(6),"4848");
            String httpUrl = matcher.group(8);

            if(httpUrl==null) {
                // defaulted
                httpUrl = "http://"+remoteHost+":8080/";
            }

            return new GlassfishContainer(
                wsimport, wsgen, new URL(httpUrl), remoteHost, Integer.parseInt(remotePort), userName, password
            );
        }

        if (lwhs) {
            System.err.println("Using the built-in Java SE lightweight HTTP server");
            Set<String> unsupportedUses = new HashSet<String>();
            unsupportedUses.add("servlet");
            unsupportedUses.add("multi-endpoint");
            if (jaxwsInJDK) {
                unsupportedUses.add("ri-api");
            }
            return new JavaSeContainer(wsimport,wsgen,port,unsupportedUses);
        }

        if(legacyLocalTransport) {
            System.err.println("Using the legacy local transport. This will be removed in a near future");
            return new LocalApplicationContainer(wsimport,wsgen);
        }

        System.err.println("Testing with the in-vm transport");
        return new InVmContainer(wsimport,wsgen);
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
            try {
                TestDescriptor td[] = new TestDescriptor[2];
                if (wsGenMode == WsGenMode.ALWAYS) {
                    td[0] = new TestDescriptor(descriptor, false, jaxwsInJDK);
                } else if (wsGenMode == WsGenMode.BOTH ) {
                    td[0] = new TestDescriptor(descriptor, false, jaxwsInJDK);
                    td[1] = new TestDescriptor(descriptor, true, jaxwsInJDK);
                } else if (wsGenMode == WsGenMode.IGNORE) {
                    td[0] = new TestDescriptor(descriptor, true, jaxwsInJDK);
                } else {
                    throw new RuntimeException("Shouldn't happen. WsGenMode="+wsGenMode);
                }
                if (version != null && !td[0].applicableVersions.isApplicable(version)) {
                    System.err.println("Skipping "+dir);
                } else {
                    suite.addTest(td[0].build(container,wsimport,clientScriptName,concurrentSideEffectFree,version));
                    if (td[1] != null)
                        suite.addTest(td[1].build(container,wsimport,clientScriptName,concurrentSideEffectFree,version));
                }
            } catch (IOException e) {
                // even if we fail to process this descriptor, don't let the whole thing fail.
                // just report that failure as a test failure.
                suite.addTest(new FailedTest("invalid descriptor",e));
            } catch (DocumentException e) {
                suite.addTest(new FailedTest("invalid descriptor",e));
            } catch (ParserConfigurationException e) {
                suite.addTest(new FailedTest("invalid descriptor",e));
            } catch (SAXException e) {
                suite.addTest(new FailedTest("invalid descriptor",e));
            }
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

    /**
     * Determines the 'home' directory of the test harness.
     * This is used to determine where to load other files.
     */
    private static File getHarnessJarDirectory() {
        try {
            String res = Main.class.getClassLoader().getResource("com/sun/xml/ws/test/Main.class").toExternalForm();
            if(res.startsWith("jar:")) {
                res = res.substring(4,res.lastIndexOf('!'));
                return new File(new URL(res).getFile()).getParentFile();
            }
            return new File(".").getAbsoluteFile();
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    /**
     * Find the nearest ancestor directory that has the given name and returns it.
     * Otherwise null.
     */
    private File getParentWithName(File file, String name) {
        while(file!=null) {
            if(file.getName().equals(name))
                return file;
            file = file.getParentFile();
        }
        return null;
    }

    /**
     * Find the nearest ancestor directory that has the given file and returns it.
     * Otherwise null.
     */
    private File getParentWithFile(File file, String markerFile) {
        while(file!=null) {
            if(new File(file,markerFile).exists())
                return file;
            file = file.getParentFile();
        }
        return null;
    }

    private static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File path) {
            return path.isDirectory();
        }
    };
}
