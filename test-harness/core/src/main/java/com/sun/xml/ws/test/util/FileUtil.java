/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.xml.ws.test.util;

import com.sun.xml.ws.test.World;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Class for utility methods for finding files and
 * information about them.
 */
public class FileUtil {

    public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File path) {
            return path.isDirectory();
        }
    };

    public static final FileFilter JAR_FILE_FILTER = new FileFilter() {
        public boolean accept(File path) {
            return path.getName().endsWith(".jar");
        }
    };

    /**
     * This method returns the fully qualified names of
     * class files below the given directory.
     */
    public static String [] getClassFileNames(File dir) {
        List<String> names = new ArrayList<String>();
        Stack<String> pathStack = new Stack<String>();
        addClassNames(dir, pathStack, names);
        return names.toArray(new String [names.size()]);
    }

    /**
     * Recursive method for adding class names under
     * a given top directory.
     */
    private static void addClassNames(File current, Stack<String> stack,
                                      List<String> names) {

        File[] children = current.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                stack.push(child.getName());
                addClassNames(child, stack, names);
                stack.pop();
            } else {
                if (child.getName().endsWith(".class")) {
                    names.add(createFullName(stack, child));
                }
            }
        }
    }

    /*
    * Create a fully-qualified path name.
    */
    private static String createFullName(Stack<String> dirs, File classFile) {
        String className = classFile.getName().substring(
            0, classFile.getName().indexOf(".class"));
        if (dirs.empty()) {
            return className;
        }
        StringBuilder fullName = new StringBuilder();
        for (String dir : dirs) {
            fullName.append(dir);
            fullName.append(".");
        }
        fullName.append(className);
        return fullName.toString();
    }

    /**
     * Recursively delete a directory and all its descendants.
     */
    public static void deleteRecursive(File dir) {
        Delete d = new Delete();
        d.setProject(World.project);
        d.setDir(dir);
        d.execute();
    }

    /**
     * Copies a single file.
     */
    public static void copyFile(File src, File dest) {
        Copy cp = new Copy();
        cp.setOverwrite(true);
        cp.setProject(World.project);
        cp.setFile(src);
        cp.setTofile(dest);
        cp.execute();
    }

    /**
     * Copies a whole directory recursively.
     */
    public static void copyDir(File src, File dest, String excludes) {
        Copy cp = new Copy();
        cp.setProject(World.project);
        cp.setTodir(dest);
        FileSet fs = new FileSet();
        if (excludes != null) {
            fs.setExcludes(excludes);
        }
        fs.setDir(src);
        cp.addFileset(fs);
        cp.execute();
    }

    public static File createTmpDir( boolean scheduleDeleteOnVmExit ) throws IOException {
        // create a temporary directory
        File dir = new File(".");
        File targetFolder = new File(dir, "target");
        if (targetFolder.exists()) {
            dir = targetFolder;
        }
        File tmpFile = File.createTempFile("wstest", "tmp", dir);
        tmpFile.delete();
        tmpFile.mkdir();
        if(scheduleDeleteOnVmExit) {
            tmpFile.deleteOnExit();
        }
        return tmpFile;
    }
}
