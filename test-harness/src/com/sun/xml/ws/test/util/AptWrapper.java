package com.sun.xml.ws.test.util;

import com.sun.xml.ws.test.World;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Path;

import java.io.File;


public final class AptWrapper extends Javac {
    public AptWrapper () {
        Project p = new Project();
        p.init();
        
        setProject(p);
        setTaskType("apt");
        setTaskName("apt");

        setCompiler(AptAdapter.class.getName());
    }
    public void init(String sourceDirName, String destDirName) {
        Path path = new Path(getProject());
        path.setPath(sourceDirName);
        setSrcdir(path);
        setDestdir(new File(destDirName));

        Path classPath = new Path(getProject());
        classPath.setPath(World.runtimeClasspath + ":" + World.toolClasspath);
        setClasspath(classPath);
    }
}
