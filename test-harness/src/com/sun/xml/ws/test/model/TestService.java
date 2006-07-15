package com.sun.xml.ws.test.model;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A service to be deployed for a test.
 *
 * <p>
 * TODO: We need to be able to mark service as an EJB service
 *
 * @author Kohsuke Kawaguchi
 */
public class TestService {

    /**
     * Name of the service.
     *
     * The name must be:
     * <ol>
     *  <li>Unique within {@link TestDescriptor}
     *  <li>an empty string or a valid Java identifier.
     * </ol>
     *
     * <p>
     * An empty string is convenient to describe the default/primary service
     * (or when there's just one service involved, which is the majority.)
     */
    @NotNull
    public final String name;

    /**
     * Directory in which the service's source files reside.
     */
    @NotNull
    public final File baseDir;
    
    /**
     * Optional WSDL file that describes this service.
     */
    @Nullable
    public final File wsdl;

    /**
     * Possibly empty list of JAXB/JAX-WS external binding customizations.
     *
     * Must be empty when {@link #wsdl} is null.
     */
    @NotNull
    public final List<File> customizations = new ArrayList<File>();

    /**
     * {@link TestEndpoint}s that this service exposes.
     *
     * <p>
     * The harness uses this information to inject proxies to the client.
     */
    @NotNull
    public final Set<TestEndpoint> endpoints = new LinkedHashSet<TestEndpoint>();

    public final TestDescriptor parent;

    public TestService(TestDescriptor parent, String name, File baseDir, File wsdl) throws IOException {
        this.parent = parent;
        this.name = name;
        this.wsdl = wsdl;
        this.baseDir = baseDir;

        // search for classes with @WebService
        findEndpoints(baseDir);
    }

    /**
     * Scans the Java source code in the server directory and
     * find all classes with @WebService. Those are turned into
     * {@link TestEndpoint}. 
     */
    private void findEndpoints(File dir) throws IOException {
        File[] dirs = dir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.isDirectory();
            }
        });
        for (File subdir : dirs) {
            findEndpoints(subdir);
        }

        File[] javas = dir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.getName().endsWith(".java");
            }
        });
        for (File src : javas) {
            // parse the Java file, looking for @WebService
            // this isn't very robust, but works without compiling them
            // (note that at this point those source files by themselves won't compile)
            BufferedReader r = new BufferedReader(new FileReader(src));
            String line;

            String pkg=null;
            boolean isWebService = false;

            while((line=r.readLine())!=null) {
                if(line.startsWith("package ")) {
                    pkg = line.substring(8,line.indexOf(';'));
                }
                if(line.contains("@WebService") || line.contains("@javax.jws.WebService"))
                    isWebService = true;
            }

            r.close();

            if(isWebService) {
                // found it!

                String className = src.getName();
                className = className.substring(0,className.length()-5); // trim off ".java"

                if(pkg!=null)
                    className = pkg+'.'+className;

                endpoints.add(new TestEndpoint(src.getName(),className));
            }
        }
    }

    public String toString() {
        return name+" of "+parent.toString();
    }
}
