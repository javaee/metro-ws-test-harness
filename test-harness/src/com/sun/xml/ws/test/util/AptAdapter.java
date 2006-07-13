package com.sun.xml.ws.test.util;

import com.sun.istack.test.Which;
import com.sun.xml.ws.test.World;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;

import java.lang.reflect.Method;

/**
 * {@link CompilerAdapter} that loads apt from another classloader.
 *
 * <p>
 * The default adapter in Ant assumes that tools.jar is in the current classloader,
 * which is not the case here.
 *
 * @author WSIT Test Harness Team
 */
public final class AptAdapter extends JDKToolAdapter {
    protected String getMainMethod() {
        return "process";
    }

    protected String getMainClass() {
        return "com.sun.tools.apt.Main";
    }
}
