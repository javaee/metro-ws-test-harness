package com.sun.xml.ws.test.util;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.XMLOutput;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.net.URL;

/**
 * Jelly driver.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Jelly {

    private final JellyContext context = new JellyContext();

    private final URL script;

    public Jelly(Class baseClass, String resourceName) {
        this(baseClass.getResource(resourceName));
    }

    public Jelly(URL script) {
        this.script = script;
    }

    /**
     * Exports a variable to the jelly script
     */
    public void set( String name, Object value ) {
        context.setVariable(name,value);
    }

    /**
     * Runs the jelly script and generates the result into a file.
     */
    public void run( File output ) throws Exception {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(output));
        try {
            XMLOutput xo = XMLOutput.createXMLOutput(os);
            context.runScript(script,xo);
            xo.flush();
        } finally {
            os.close();
        }
    }
}
