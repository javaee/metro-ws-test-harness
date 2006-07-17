package com.sun.xml.ws.test.util;

/**
 * Ant task that invokes {@code APT} loaded in a separate classloader.
 *
 * @author Kohsuke Kawaguchi
 */
public final class AptTask extends AbstractJavacTask {
    public AptTask() {
        super("APT", AptAdapter.class);
    }

    public static final class AptAdapter extends JDKToolAdapter {
        protected String getMainMethod() {
            return "process";
        }

        protected String getMainClass() {
            return "com.sun.tools.apt.Main";
        }

        protected String getToolName() {
            return "APT";
        }
    }
}