/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.xml.ws.test;

import com.sun.istack.Nullable;
import org.apache.tools.ant.loader.AntClassLoader2;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Represents a classloader.
 *
 * {@link Realm}s form a tree structure where children delegates
 * to the parent for classloading.
 *
 * @author Kohsuke Kawaguchi
 */
public class Realm {
    /**
     * Human readable name that identifies this realm for the debuggin purpose.
     */
    private final String name;

    /**
     * Parent realm. Class loading delegates to this parent.
     */
    private final @Nullable Realm parent;

    /**
     * Jar files and class folders that are added.
     */
    private final Path classPath = new Path(World.project);

    private AntClassLoader2 classLoader;

    public Realm(String name, Realm parent) {
        this.name = name;
        this.parent = parent;
    }

    public synchronized ClassLoader getClassLoader() {
        if(classLoader==null) {
            // delegates to the system classloader by default.
            // when invoked for debugging harness (with a lot of jars in system classloader),
            // this provides the easy debug path.
            // when invoked through bootstrap, this still provides the maximum isolation.
            ClassLoader pcl = ClassLoader.getSystemClassLoader();
            if(parent!=null)
                pcl = parent.getClassLoader();
            classLoader = new AntClassLoader2(); 
            classLoader.setParent(pcl);
            classLoader.setProject(World.project);
            classLoader.setClassPath(classPath);
            classLoader.setDefaultAssertionStatus(true);
        }

        return classLoader;
    }

    /**
     * Adds a single jar.
     */
    public void addJar(File jar) throws IOException {
        assert classLoader==null : "classLoader is already created";
        if(!jar.exists())
            throw new IOException("No such file: "+jar);
        classPath.createPathElement().setLocation(jar);
    }

    /**
     * Adds a single class folder.
     */
    public void addClassFolder(File classFolder) throws IOException {
        addJar(classFolder);
    }

    /**
     * Adds all jars in the given folder.
     *
     * @param folder
     *      A directory that contains a bunch of jar files.
     * @param excludes
     *      List of jars to be excluded
     */
    public void addJarFolder(File folder, final String... excludes) throws IOException {
        if(!folder.isDirectory())
            throw new IOException("Not a directory "+folder);

        File[] children = folder.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                for (String name : excludes) {
                    if(pathname.getName().equals(name))
                        return false;   // excluded
                }
                return pathname.getPath().endsWith(".jar");
            }
        });

        for (File child : children) {
            addJar(child);
        }
    }

    public void dump(PrintStream out) {
        for( String item : classPath.toString().split(File.pathSeparator)) {
            out.println("  "+item);
        }
    }

    public String toString() {
        return name+" realm";
    }

    /**
     * List all the components in this realm (excluding those defined in parent.)
     */
    public File[] list() {
        String[] names = classPath.list();
        File[] r = new File[names.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = new File(names[i]);
        }
        return r;
    }

    public Path getPath() {
        return classPath;
    }

    public Class loadClass(String className) throws ClassNotFoundException {
        return getClassLoader().loadClass(className);
    }
}
