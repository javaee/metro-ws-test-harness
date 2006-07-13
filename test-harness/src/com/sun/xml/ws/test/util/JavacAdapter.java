package com.sun.xml.ws.test.util;

import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;

/**
 * {@link CompilerAdapter} that loads javac from another classloader.
 *
 * <p>
 * The default adapter in Ant assumes that tools.jar is in the current classloader,
 * which is not the case here.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JavacAdapter extends JDKToolAdapter {
    protected String getMainMethod() {
        return "compile";
    }

    protected String getMainClass() {
        return "com.sun.tools.javac.Main";
    }
}
