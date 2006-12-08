package com.sun.xml.ws.test.model;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

import java.io.*;
import java.util.*;

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
    public final WSDL wsdl;

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
    /**
     * Determines if the service is an STS for WSTrust and needs special handling
     */
    public boolean isSTS;

    public TestService(TestDescriptor parent, String name, File baseDir, WSDL wsdl, boolean sts) throws IOException {
        this.parent = parent;
        this.name = name;
        this.wsdl = wsdl;
        this.baseDir = baseDir;
        this.isSTS = sts;

        // search for classes with @WebService
        findEndpoints(baseDir);
    }

    /**
     * Gets the {@link TestEndpoint} that has the specified implementation class.
     */
    public TestEndpoint getEndpointByImpl(String implClassFullName) {
        for (TestEndpoint ep : endpoints) {
            if(ep.className.equals(implClassFullName))
                return ep;
        }
        throw new Error("No TestEndpoint object recorded for "+implClassFullName);
    }

    /**
     * Returns the name combined with the test case name to make a globaly unique name
     * for this service.
     */
    public String getGlobalUniqueName() {
        if(name.length()==0)
            return parent.name;
        else
            return parent.name +'.'+name;
    }

    /**
     * Scans the Java source code in the server directory and
     * find all classes with @WebService. Those are turned into
     * {@link TestEndpoint}. 
     */
    private void findEndpoints(File dir) throws IOException {
        File[] dirs = dir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                // don't go in our own work directory
                return child.isDirectory() && !child.getName().equals("work");
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
            boolean isInterface = false;

            OUTER:
            while((line=r.readLine())!=null) {
                if(line.startsWith("package ")) {
                    pkg = line.substring(8,line.indexOf(';'));
                }
                if(line.contains("@WebServiceProvider") || line.contains("@javax.xml.ws.WebServiceProvider"))
                    isWebService = true;
                else if(line.contains("@WebService") || line.contains("@javax.jws.WebService"))
                    isWebService = true;

                // if we read until the first declared type, we should have found all the
                // @WebService* annoations and package declaration.
                // reading it furhter is pointless and dangerous as we may hit
                // inner interface/classes
                if(line.contains("public interface") || line.contains("public class")) {
                    StringTokenizer stk = new StringTokenizer(line);
                    while(stk.hasMoreTokens()) {
                        String tk = stk.nextToken();
                        if(tk.equals("interface")) {
                            isInterface = true;
                            break OUTER;
                        }
                        if(tk.equals("class")) {
                            isInterface = false;
                            break OUTER;
                        }
                    }
                }
            }

            r.close();
            // if isInterface=true && isWebService=true, means it is SEI
            if(isWebService && !isInterface) {
                // found it!

                String className = src.getName();
                className = className.substring(0,className.length()-5); // trim off ".java"

                String fullName;

                if(pkg!=null)
                    fullName = pkg+'.'+className;
                else
                    fullName = className;

                endpoints.add(new TestEndpoint(className,fullName));
            }
        }
    }

    public String toString() {
        return name+" of "+parent.toString();
    }

    /**
     * This filter gives all handler configuration files in the directory.
     * i.e files matching pattern *handlers.xml
     */
    class HandlersFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith("handlers.xml"));
        }
    }

    public File[] getHandlerConfiguration() {
        return baseDir.listFiles(new HandlersFilter());
    }


}
