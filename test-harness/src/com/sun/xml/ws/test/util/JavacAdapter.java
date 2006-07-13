package com.sun.xml.ws.test.util;

import com.sun.istack.test.Which;
import com.sun.xml.ws.test.World;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;

import java.lang.reflect.Method;

/**
 * {@link CompilerAdapter} that loads javac from another classloader.
 *
 * <p>
 * The default adapter in Ant assumes that tools.jar is in the current classloader,
 * which is not the case here.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JavacAdapter extends DefaultCompilerAdapter {
    /**
     * Run the compilation.
     *
     * @exception BuildException if the compilation has problems.
     */
    public boolean execute() throws BuildException {
        Commandline cmd = setupModernJavacCommand();

        try {
            int result = (Integer) getJavacMain().invoke(null, (Object)cmd.getArguments());
            return result == 0;
        } catch (Exception ex) {
            throw new BuildException("Error compiling code", ex);
        }
    }

    private Method getJavacMain() {
        try {
            Method m = World.tool.getClassLoader()
                .loadClass("com.sun.tools.javac.Main")
                .getMethod("compile", String[].class);
            System.out.println("Using javac from "+ Which.which(m.getDeclaringClass()));
            return m;
        } catch( Throwable e ) {
            e.printStackTrace();
            throw new AssertionError("Unable to find javac in the same VM. Have you set JAVA_HOME?");
        }

    }
}
