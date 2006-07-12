package com.sun.xml.ws.test.util;

import com.sun.istack.test.Which;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.types.Commandline;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * {@link CompilerAdapter} that loads apt from another classloader.
 *
 * <p>
 * The default adapter in Ant assumes that tools.jar is in the current classloader,
 * which is not the case here.
 *
 * @author WSIT Test Harness Team
 */
public final class AptAdapter extends DefaultCompilerAdapter {
    /**
     * Run the compilation.
     *
     * @exception BuildException if the compilation has problems.
     */
    public boolean execute() throws BuildException {
        Commandline cmd = setupModernJavacCommand();

        try {
            int result = (Integer) apt.invoke(null, (Object)cmd.getArguments());
            return result == 0;
        } catch (Exception ex) {
            throw new BuildException("Error compiling code", ex);
        }
    }

    /**
     * Returns a class loader that can load classes from JDK tools.jar.
     */
    private static ClassLoader getToolsJarLoader() {
        // if it fails, try to locate apt from java.home
        File jreHome = new File(System.getProperty("java.home"));
        File toolsJar = new File( jreHome.getParent(), "lib/tools.jar" );

        try {
            return new URLClassLoader(
                    new URL[]{ toolsJar.toURL() }, null );
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    /**
     * apt's compile method.
     */
    private static final Method apt = initApt();

    private static Method initApt() {
        try {
            Method m = getToolsJarLoader()
                .loadClass("com.sun.tools.apt.Main")
                .getMethod("main", String[].class);
            System.out.println("Using apt from "+ Which.which(m.getDeclaringClass()));
            return m;
        } catch( Throwable e ) {
            e.printStackTrace();
            throw new AssertionError("Unable to find apt in the same VM. Have you set JAVA_HOME?");
        }

    }
}
