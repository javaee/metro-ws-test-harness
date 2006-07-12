package com.sun.xml.ws.test.client;

import bsh.Interpreter;
import bsh.NameSpace;
import bsh.EvalError;

import java.io.Reader;
import java.io.PrintStream;

/**
 * {@link Interpreter} that redirects stderr to stdout.
 *
 * We don't want scripts from cluttering stderr, which is
 * reserved for the test harness.
 */
public class InterpreterEx extends Interpreter {

    // there's a bug in BeanShell (ClassManagerImpl.plainClassForName)
    // that makes BeanShell refer to ContextClassLoader as opposed to
    // the class loader specified in the setClassLoader method.
    // to work around the problem, set the class loader when
    // evaluating scripts.
    //
    // see http://sourceforge.net/tracker/index.php?func=detail&aid=864572&group_id=4075&atid=104075
    // for the bug report
    private ClassLoader externalClassLoader;

    public InterpreterEx(ClassLoader externalClassLoader) {
        this.externalClassLoader = externalClassLoader;
    }

    public void setClassLoader(ClassLoader classLoader){
        super.setClassLoader(classLoader);
        this.externalClassLoader =classLoader;
    }

    private ClassLoader replaceContextClassLoader() {
        Thread t = Thread.currentThread();
        ClassLoader oldContextClassLoader = t.getContextClassLoader();
        if(externalClassLoader!=null)
            t.setContextClassLoader(externalClassLoader);
        return oldContextClassLoader;
    }

    public Object eval(Reader in, NameSpace nameSpace, String sourceFileInfo) throws EvalError {
        ClassLoader old = replaceContextClassLoader();
        PrintStream stderr = System.err;
        try {
            System.setErr(System.out);
            return super.eval(in, nameSpace, sourceFileInfo);
        } finally {
            System.setErr(stderr);
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public Object eval(Reader in) throws EvalError {
        ClassLoader old = replaceContextClassLoader();
        PrintStream stderr = System.err;
        try {
            System.setErr(System.out);
            return super.eval(in);
        } finally {
            System.setErr(stderr);
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public Object eval(String statements, NameSpace nameSpace) throws EvalError {
        ClassLoader old = replaceContextClassLoader();
        PrintStream stderr = System.err;
        try {
            System.setErr(System.out);
            return super.eval(statements, nameSpace);
        } finally {
            System.setErr(stderr);
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public Object eval(String statements) throws EvalError {
        ClassLoader old = replaceContextClassLoader();
        PrintStream stderr = System.err;
        try {
            System.setErr(System.out);
            return super.eval(statements);
        } finally {
            System.setErr(stderr);
            Thread.currentThread().setContextClassLoader(old);
        }
    }
}
