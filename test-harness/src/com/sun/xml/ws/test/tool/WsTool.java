package com.sun.xml.ws.test.tool;

import com.sun.xml.ws.test.World;
import junit.framework.Assert;

import java.io.File;

/**
 * Interface to the <tt>wsimport</tt> or <tt>wsgen</tt> command-line tool.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WsTool extends Assert {
    /**
     * Invokes wsimport with the given arguments.
     *
     * @throws Exception
     *      if the compilation fails.
     *      alternatively, JUnit {@link Assert}ions can be used
     *      to detect error conditions.
     */
    public abstract void invoke(String... args) throws Exception;

    /**
     * Returns true if the '-skip' mode is on and
     * the tool invocation is skipped.
     */
    public boolean isNoop() {
        return this==NOOP;
    }

    /**
     * Determines which compiler to use.
     *
     * @param externalWsImport
     *      null to run {@link WsTool} from {@link World#tool}.
     *      Otherwise this file will be the path to the script.
     */
    public static WsTool createWsImport(File externalWsImport) {
        return createTool(externalWsImport, "com.sun.tools.ws.WsImport");
    }

    /**
     * Determines which wsgen to use.
     *
     * @param externalWsGen
     *      null to run {@link WsTool} from {@link World#tool}.
     *      Otherwise this file will be the path to the script.
     */
    public static WsTool createWsGen(File externalWsGen) {
        return createTool(externalWsGen,"com.sun.tools.ws.WsGen");
    }

    private static WsTool createTool(File externalExecutable, String className) {
        if(externalExecutable !=null) {
            return new RemoteWsTool(externalExecutable);
        } else {
            return new LocalWsTool(className);
        }
    }

    /**
     * {@link WsTool} that does nothing.
     *
     * <p>
     * This assumes that files that are supposed to be generated
     * are already generated.
     */
    public static WsTool NOOP = new WsTool() {
        public void invoke(String... args) {
        }
    };
}
