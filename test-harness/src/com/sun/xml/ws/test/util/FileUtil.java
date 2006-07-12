package com.sun.xml.ws.test.util;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Class for utility methods for finding files and
 * information about them.
 */
public class FileUtil {

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
        StringBuffer fullName = new StringBuffer();
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
        Project p = new Project();
        p.init();
        Delete d = new Delete();
        d.setProject(p);
        d.setDir(dir);
        d.execute();
    }
}
