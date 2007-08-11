package com.sun.xml.ws.test.exec;

import bsh.Interpreter;
import bsh.NameSpace;
import bsh.EvalError;
import bsh.TargetError;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.client.ScriptBaseClass;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.model.TestClient;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.beans.Introspector;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
        ns.importPackage("javax.xml.ws.soap");
        ns.importPackage("javax.xml.ws.handler");
        ns.importPackage("javax.xml.ws.handler.soap");
        ns.importPackage("javax.xml.bind");
        ns.importPackage("javax.xml.soap");
        ns.importPackage("javax.xml.namespace");
        ns.importPackage("javax.xml.transform");
        ns.importPackage("javax.xml.transform.sax");
        ns.importPackage("javax.xml.transform.dom");
        ns.importPackage("javax.xml.transform.stream");
        ns.importPackage("java.util");
        ns.importPackage("java.util.concurrent");
        // if there's any client package, put them there
        ns.importPackage(context.descriptor.name+".client");

        // this will make 'thisObject' available as 'this' in script
        ns.importObject(new ScriptBaseClass(context, engine, client));

        // load additional helper methods
        try {
            engine.eval(new InputStreamReader(getClass().getResourceAsStream("util.bsh")));
        } catch (EvalError evalError) {
            throw new Error("Failed to evaluate util.bsh",evalError);
        }

        // when invoking JAX-WS, we need to set the context classloader accordingly
        // so that it can discover classes from the right places.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(context.clientClassLoader);

        try {
            injectResources(ns, engine);
            invoke(engine);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Makes the test script invocation.
     */
    protected void invoke(Interpreter engine) throws Throwable {
        // executes the script
        Reader r = client.script.read();
        try {
            engine.eval(r, engine.getNameSpace(), client.script.getName() );
        } catch(TargetError e) {
            throw e.getTarget();
        } finally {
            r.close();
        }
    }

    private void injectResources(NameSpace ns, Interpreter engine) throws Exception {
        StringBuilder serviceList = new StringBuilder("injected services:");
        StringBuilder portList = new StringBuilder("injected ports:");
        StringBuilder addressList = new StringBuilder("injected addresses:");

        // inject test home directory
        engine.set("home",client.parent.home);

        for (DeployedService svc : context.services.values()) {
            if (! svc.service.isSTS) {
                for (Class clazz : svc.serviceClass) {
                    String packageName = clazz.getPackage().getName();
                    //  import the artifact package
                    ns.importPackage(packageName);
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

                    Object serviceInstance = clazz.newInstance();

                    {// inject a service instance
                        String serviceVarName = Introspector.decapitalize(clazz.getSimpleName());
                        engine.set(serviceVarName,serviceInstance);
                        serviceList.append(' ').append(serviceVarName);
                    }

                    for (Method method : methods) {
                        Annotation endpoint = method.getAnnotation(webendpointAnnotation);
						// don't inject variables for methods like getHelloPort(WebServiceFeatures)
                        if (endpoint != null && method.getParameterTypes().length == 0) {

                            //For multiple endpoints the convention for injecting the variables is
                            // portName obtained from the WebEndpoint annotation,
                            // which would be something like "addNumbersPort"
                            String portName = Introspector.decapitalize((String)nameMethod.invoke(endpoint));

                            try {
                                engine.set(portName, method.invoke(serviceInstance));
                                engine.set(portName+"Address",svc.app.getEndpointAddress((TestEndpoint)svc.service.endpoints.toArray()[0]));
                                addressList.append(' ').append(portName+"Address");
                            } catch (InvocationTargetException e) {
                                if(e.getCause() instanceof Exception)
                                    throw (Exception)e.getCause();
                                else
                                    throw e;
                            }
                            portList.append(' ').append(portName);
                        }
                    }
                }
            }
        }
        System.out.println(serviceList);
        System.out.println(portList);
        System.out.println(addressList);
    }
}
