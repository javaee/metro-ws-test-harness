package com.sun.xml.ws.test;

import org.codehaus.classworlds.ClassRealm;

import java.io.File;
import java.io.IOException;
import java.io.FileFilter;
import java.io.PrintStream;
import java.util.Arrays;

import com.sun.xml.ws.test.World;

/**
 * Adds jar files to {@link ClassRealm}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class RealmBuilder {
    private final ClassRealm realm;
    private String classPath;

    public RealmBuilder(ClassRealm realm, String classPath) {
        this.realm = realm;
        this.classPath = classPath;
    }

    /**
     * Adds a single jar.
     */
    public void addJar(File jar) throws IOException {
        if(!jar.exists())
            throw new IOException("No such file: "+jar);
        realm.addConstituent(jar.toURL());

        if (classPath != null) {
            classPath += File.pathSeparatorChar + jar.toString();
        } else {
            classPath = jar.toString();
        }
    }
   
    public String getClasspath() {
        return classPath;
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
        for( String item : classPath.split(File.pathSeparator)) {
            out.println("  "+item);
        }
    }
}
