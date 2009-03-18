/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.istack.test.VersionRequirement;
import com.sun.istack.test.VersionNumber;
import com.sun.istack.test.VersionProcessor;

import java.beans.Introspector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Execute Java client.
 *
 * @author Kohsuke Kawaguchi
 */
public class JavaClientExecutor extends Executor {
    /**
     * JUnit test class name.
     */
    private final String testClassName;

    private final File testSourceFile;

    private final int testCount;

    private final boolean skipTest;
    public JavaClientExecutor(DeploymentContext context, File sourceFile, VersionNumber version) throws IOException {
        super(cutExtension(sourceFile.getName()), context);
        this.testSourceFile = sourceFile;
        String packageName=null;
        int count=0;
        BufferedReader in = new BufferedReader(new FileReader(testSourceFile));
        String line;
        VersionProcessor versionProcessor = null;
        while((line=in.readLine())!=null) {
            line = line.trim();
            if(line.startsWith("package ")) {
                line = line.substring("package ".length());
                packageName = line.substring(0,line.indexOf(';'));
            }
            if(line.startsWith("@VersionRequirement") || line.startsWith("@com.sun.istack.test.VersionRequirement")) {
                versionProcessor = new VersionProcessor(grabAttributeValue(line,"since"),
                        grabAttributeValue(line,"until"),
                        grabAttributeValue(line,"excludeFrom"));
            }
            if(line.startsWith("public void test"))
                count++;
        }
        this.testClassName = packageName+'.'+cutExtension(sourceFile.getName());
        if(version != null && versionProcessor!=null && !versionProcessor.isApplicable(version)) {
            skipTest = true;
            this.testCount = 0;
        } else {
            skipTest = false;
            this.testCount = count;
        }
    }

    private String grabAttributeValue(String str, String attr) {
        String patternStr=attr+"\\s*=\\s*\"(.+)\"";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(str);
        if(matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    private static String cutExtension(String name) {
        int idx = name.lastIndexOf('.');
        name = name.substring(0,idx);
        return name;
    }

    @Override
    public int countTestCases() {
        return testCount;
    }

    @Override
    public void run(TestResult result) {
        if(skipTest) {
            System.out.printf("Version Requirement not satisfied, Skipping Test"+ testClassName);
            return;
        }
        if(context.clientClassLoader==null) {
            failAll(result,"this test is skipped because of other failures",null);
            return;
        }

        // when invoking JAX-WS, we need to set the context classloader accordingly
        // so that it can discover classes from the right places.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(context.clientClassLoader);

        try {
            TestSuite ts;
            try {
                injectResources();
                Class<?> testClass = context.clientClassLoader.loadClass(testClassName);

                // let JUnit parse all the test cases
                ts = new TestSuite(testClass);

                for(int i=0; i<ts.testCount(); i++) {
                    Test t = ts.testAt(i);
                    inject(t);
                }
            } catch (Exception e) {
                failAll(result,"failed to prepare JUnit test class "+testClassName,e);
                return;
            }
            
            ts.run(result);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void runBare() throws Throwable {
        // should never be here
        throw new AssertionError();
    }

    private void failAll(TestResult result, final String msg, final Exception error) {
        for(int i=0;i<testCount;i++) {
            new TestCase(testClassName) {
                @Override
                protected void runTest() throws Exception {
                    System.out.println(msg);
                    throw new Exception(msg,error);
                }
            }.run(result);
        }
    }

    private boolean hasWebServiceRef(Field f) {
        for (Annotation a : f.getAnnotations()) {
            if(a.annotationType().getName().equals("javax.xml.ws.WebServiceRef"))
                return true;
        }
        return false;
    }

    private void inject(Object o) throws Exception {
        OUTER:
        for (Field f : o.getClass().getFields()) {
            if(hasWebServiceRef(f)) {
                // determine what to inject
                Class type = f.getType();

                for (DeployedService svc : context.services.values()) {
                    for (Class clazz : svc.serviceClass) {
                        if(type.isAssignableFrom(clazz)) {
                            f.set(o,clazz.newInstance());
                            continue OUTER;
                        }

                        for (Method method : clazz.getMethods()) {
                            if (type.isAssignableFrom(method.getReturnType())
                             && method.getParameterTypes().length==0) {
                                f.set(o,method.invoke(clazz.newInstance()));
                                continue OUTER;
                            }
                        }
                    }
                }
            }
        }
    }

    private void injectResources() throws Exception {
        StringBuilder addressList = new StringBuilder("injected addresses:");

        Properties properties = System.getProperties();

        for (DeployedService svc : context.services.values()) {
            if (! svc.service.isSTS) {
                for (Class clazz : svc.serviceClass) {
                    String packageName = clazz.getPackage().getName();
                    //  use reflection to list up all methods with 'javax.xml.ws.WebEndpoint' annotations
                    //  invoke that method via reflection to obtain the Port object.
                    //  set the endpoint address to that port object
                    //  inject it to the scripting engine
                    Method[] methods = clazz.getMethods();

                    // annotation that serviceClass loads and annotation that this code
                    // uses might be different
                    Class<? extends Annotation> webendpointAnnotation = clazz.getClassLoader()
                            .loadClass("javax.xml.ws.WebEndpoint").asSubclass(Annotation.class);
                    Method nameMethod = webendpointAnnotation.getDeclaredMethod("name");

                    for (Method method : methods) {
                        Annotation endpoint = method.getAnnotation(webendpointAnnotation);
						// don't inject variables for methods like getHelloPort(WebServiceFeatures)
                        if (endpoint != null && method.getParameterTypes().length == 0) {

                            //For multiple endpoints the convention for injecting the variables is
                            // portName obtained from the WebEndpoint annotation,
                            // which would be something like "addNumbersPort"
                            String portName = (String) nameMethod.invoke(endpoint);
                            String varName = Introspector.decapitalize(portName);

                            try {
                                properties.setProperty(varName +"Address",svc.app.getEndpointAddress(getEndpoint(svc, portName)).toString());
                                addressList.append(' ').append(varName).append("Address");
                            } catch (InvocationTargetException e) {
                                if(e.getCause() instanceof Exception)
                                    throw (Exception)e.getCause();
                                else
                                    throw e;
                            }
                        }
                    }
                }
            }
        }
        System.out.println(addressList);
    }

    private TestEndpoint getEndpoint(DeployedService svc, String portName) {
        // first, look for the port name match
        for (TestEndpoint e : svc.service.endpoints) {
            if(e.portName!=null && e.portName.equals(portName))
                return e;
        }
        // nothing explicitly matched.
        if(svc.service.endpoints.size()!=1)
            throw new Error("Multiple ports are defined on '"+svc.service.name+"', yet ports are ambiguous. Please use @WebService/Provider(portName=)");
        // there's only one
        return (TestEndpoint)svc.service.endpoints.toArray()[0];
    }
}