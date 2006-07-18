package com.sun.xml.ws.test.util;

import com.sun.xml.ws.test.World;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.types.Path;

import java.io.File;


public abstract class AbstractJavacTask extends Javac {
    /*package*/ AbstractJavacTask(String toolName, Class<? extends CompilerAdapter> adapterClass) {
        Project p = new Project();
        p.init();
        
        setProject(p);
        setTaskType(toolName);
        setTaskName(toolName);

        setCompiler(adapterClass.getName());

        setClasspath(World.runtimeClasspath);
    }

    /**
     * Set the source directories where java files
     * to be compiled are located.
     */
    public void setSourceDir( File... sourceDirs ) {
        Path path = new Path(getProject());
        for (File sourceDir : sourceDirs) {
            path.createPathElement().setPath(sourceDir.getAbsolutePath());
        }
        setSrcdir(path);
    }
}
