package com.sun.xml.ws.test.util;

import com.sun.xml.ws.test.World;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

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
        cp.setProject(World.project);
        cp.setFile(src);
        cp.setTofile(dest);
        cp.execute();
    }

    /**
     * Copies a whole directory recursively.
     */
    public static void copyDir(File src, File dest) {
        Copy cp = new Copy();
        cp.setProject(World.project);
        cp.setTodir(dest);
        FileSet fs = new FileSet();
        fs.setDir(src);
        cp.addFileset(fs);
        cp.execute();
    }
}
