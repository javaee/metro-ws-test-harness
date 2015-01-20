/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package com.sun.xml.ws.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to copy all necessary test artifacts out of harness (testcases directory)
 * to separate directory
 */
public class SourcesCollector {

    private static final Set IGNORED = new HashSet<String>();

    static {
        IGNORED.add(".DS_Store");
    }

    String source;

    public SourcesCollector(String source) {
        this.source = source;
    }

    public static void ensureDirectoryExists(String newDir) {
        File file = new File(newDir);
        if (!file.exists()) {
            ensureDirectoryExists(file.getParent());
            file.mkdir();
        }
    }

    public void copyFilesTo(String destDir) {
        List<String> files = collectFiles();

        for (String file : files) {
            File src = new File(source + "/" + file);
            File dst = new File(destDir + "/" + file);
            ensureDirectoryExists(dst.getParent());
            try {
                copy(src, dst);
            } catch (IOException e) {
                System.err.println("Error copying file: [" + src + "] to [" + dst + "]");
                e.printStackTrace();
            }
        }
    }

    private static void copy(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            System.out.println("copy [" + source.getAbsoluteFile() + "]\n" +
                               "  to [" + dest.getAbsoluteFile() + "].");
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length, totalLength = 0;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
                totalLength += length;
            }
            System.out.println("totalLength = " + totalLength);
        } finally {
            if (is != null) is.close();
            if (os != null) os.close();
        }

    }

    protected List<String> collectFiles() {
        File f = new File(source);
        File[] files = f.listFiles();
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            collectFile(file, result);
        }
        return result;
    }

    private void collectFile(File file, List<String> files) {
        if (file.isDirectory()) {
            for(File f : file.listFiles()) {
                collectFile(f, files);
            }
        } else {
            String result = file.getAbsoluteFile().toString().replaceAll(source, "");
            if (result.startsWith("/")) result = result.substring(1);
            if (!IGNORED.contains(result)) {
                System.out.println("file: [" + result + "]");
                files.add(result);
            }
        }
    }

}
