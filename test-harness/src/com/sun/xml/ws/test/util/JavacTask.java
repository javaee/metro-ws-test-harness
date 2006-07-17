package com.sun.xml.ws.test.util;

/**
 * Ant task that invokes {@code Javac} loaded in a separate classloader.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JavacTask extends AbstractJavacTask {
    public JavacTask() {
        super("javac", JavacAdapter.class);
    }

    public static final class JavacAdapter extends JDKToolAdapter {
        protected String getMainMethod() {
            return "compile";
        }

        protected String getMainClass() {
            return "com.sun.tools.javac.Main";
        }

        protected String getToolName() {
            return "javac";
        }
    }
}
