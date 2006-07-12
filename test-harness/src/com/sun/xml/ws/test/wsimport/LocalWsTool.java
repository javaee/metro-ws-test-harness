package com.sun.xml.ws.test.wsimport;

import com.sun.xml.ws.test.World;
import com.sun.istack.test.Which;

import java.lang.reflect.Method;

/**
 * {@link WsTool} run locally within the same VM.
 * 
 * @author Kohsuke Kawaguchi
 */
final class LocalWsTool extends WsTool {

    private final Method main;

    public LocalWsTool(String className) {
        try {
            Class wsimport = World.tool.loadClass(className);
            System.out.println("Using "+Which.which(wsimport));
            main = wsimport.getMethod("doMain",String[].class);
        } catch (ClassNotFoundException e) {
            throw new Error("Unable to find tool "+className,e);
        } catch (NoSuchMethodException e) {
            throw new Error("No main method on "+className,e);
        }
    }

    public void invoke(String... args) throws Exception {
        int r = (Integer)main.invoke(null,new Object[]{args});
        if(r!=0)
            assertEquals("wsimport reported exit code "+r, 0,r);
    }
}
