package com.sun.xml.ws.test.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * Bean shell script.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Script {
    private Script() {}

    /**
     * Name of the script.
     *
     * This is intended to allow humans to find out which script this is.
     */
    public abstract String getName();

    /**
     * Returns a new reader that reads the script.
     */
    public abstract Reader read() throws IOException;


    /**
     * {@link Script} where the script is given as literal text.
     */
    public static final class Inline extends Script {
        private final String script;

        public Inline(String script) {
            this.script = script;
        }

        /**
         * Use a portion of the script as the name.
         */
        public String getName() {
            String text = script.replace('\n',' ');
            if(text.length()>60)
                return text.substring(60)+" ...";
            else
                return text;
        }

        public Reader read() {
            return new StringReader(script);
        }
    }

    /**
     * {@link Script} where the script is stored in a file.
     */
    public static final class File extends Script {
        private final java.io.File script;

        public File(java.io.File script) {
            this.script = script;
        }

        public String getName() {
            return script.getName();    // just use the file name portion
        }

        public Reader read() throws IOException {
            return new InputStreamReader(new FileInputStream(script),"UTF-8");
        }
    }
}
