package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.DeploymentContext;
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
import java.lang.reflect.Method;

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

    public JavaClientExecutor(DeploymentContext context, File sourceFile) throws IOException {
        super(cutExtension(sourceFile.getName()), context);
        this.testSourceFile = sourceFile;

        String packageName=null;
        int count=0;
        BufferedReader in = new BufferedReader(new FileReader(testSourceFile));
        String line;
        while((line=in.readLine())!=null) {
            line = line.trim();
            if(line.startsWith("package ")) {
                line = line.substring("package ".length());
                packageName = line.substring(0,line.indexOf(';'));
            }
            if(line.startsWith("public void test"))
                count++;
        }

        this.testClassName = packageName+'.'+cutExtension(sourceFile.getName());
        this.testCount = count;
    }

    private static String cutExtension(String name) {
        int idx = name.lastIndexOf('.');
        name = name.substring(0,idx);
        return name;
    }

    public int countTestCases() {
        return testCount;
    }

    public void run(TestResult result) {
        if(context.clientClassLoader==null) {
            failAll(result,"this test is skipped because of other failures",null);
            return;
        }

        TestSuite ts=null;
        try {
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

        // when invoking JAX-WS, we need to set the context classloader accordingly
        // so that it can discover classes from the right places.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(context.clientClassLoader);

        try {
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
            new TestCase() {
                protected void runTest() throws Exception {
                    System.out.println(msg);
                    throw error;
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
}
