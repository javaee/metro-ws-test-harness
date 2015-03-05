/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.test.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

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


    public String getSource() {
        Reader reader = null;
        try {
            reader = read();
            StringWriter writer = new StringWriter();
            char [] buf = new char[1024];
            int read;
            while ((read = reader.read(buf)) != -1) {
                writer.write(buf, 0, read);
            }
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "<ERROR READING SCRIPT>";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * {@link Script} where the script is given as literal text.
     */
    public static final class Inline extends Script {
        private final String name;
        private final String script;

        public Inline(String name, String script) {
            this.name = name;
            this.script = script;
        }

        /**
         * Use a portion of the script as the name.
         */
        public String getName() {
            return name;
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
