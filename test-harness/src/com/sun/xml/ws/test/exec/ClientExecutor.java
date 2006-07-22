package com.sun.xml.ws.test.exec;

import bsh.Interpreter;
import bsh.NameSpace;
import bsh.EvalError;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.client.ScriptBaseClass;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.model.TestClient;

import java.beans.Introspector;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Executes {@link TestClient}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClientExecutor extends Executor {
    /**
     * Client test scenario to execute.
     */
    private final TestClient client;

    public ClientExecutor(DeploymentContext context, TestClient client) {
        super("client "+client.script.getName().replace('.','_'), context);
        this.client = client;
    }

    public void runBare() throws Throwable {
        if(context.clientClassLoader==null)
            fail("this test is skipped because of other failures");

        Interpreter engine = new InterpreterEx(context.clientClassLoader);

        NameSpace ns = engine.getNameSpace();
        // import namespaces. what are the other namespaces to be imported?
        ns.importPackage("javax.xml.ws");
        ns.importPackage("javax.xml.bind");
        ns.importPackage("javax.xml.namespace");

        // this will make 'thisObject' available as 'this' in script
        ns.importObject(new ScriptBaseClass(context, client));

        // when invoking JAX-WS, we need to set the context classloader accordingly
        // so that it can discover classes from the right places.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(context.clientClassLoader);

        try {
            injectResources(ns, engine);

            // executes the script
            Reader r = client.script.read();
            try {
                engine.eval(r, engine.getNameSpace(), client.script.getName() );
            } finally {
                r.close();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private void injectResources(NameSpace ns, Interpreter engine) throws Exception {
        StringBuilder buf = new StringBuilder("injected ports:");

        for (DeployedService svc : context.services.values()) {

            String packageName = svc.serviceClass.getPackage().getName();
            //  import the artifact package
            ns.importPackage(packageName);
            //  use reflection to list up all methods with 'javax.xml.ws.WebEndpoint' annotations
            //  invoke that method via reflection to obtain the Port object.
            //  set the endpoint address to that port object
            //  inject it to the scripting engine
            Method[] methods = svc.serviceClass.getMethods();

            // annotation that serviceClass loads and annotation that this code
            // uses might be different
            Class<? extends Annotation> webendpointAnnotation = svc.serviceClass.getClassLoader()
                .loadClass("javax.xml.ws.WebEndpoint").asSubclass(Annotation.class);
            Method nameMethod = webendpointAnnotation.getDeclaredMethod("name");

            Object serviceInstance = svc.serviceClass.newInstance();

            for (Method method : methods) {
                Annotation endpoint = method.getAnnotation(webendpointAnnotation);
                if (endpoint != null) {
                    //For multiple endpoints the convention for injecting the variables is
                    // portName obtained from the WebEndpoint annotation,
                    // which would be something like "addNumbersPort"
                    String portName = Introspector.decapitalize((String)nameMethod.invoke(endpoint));

                    try {
                        engine.set(portName, method.invoke(serviceInstance));
                    } catch (InvocationTargetException e) {
                        if(e.getCause() instanceof Exception)
                            throw (Exception)e.getCause();
                        else
                            throw e;
                    }
                    buf.append(' ').append(portName);
                }
            }
        }

        System.out.println(buf);
    }
}
