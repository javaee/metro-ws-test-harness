/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.xml.ws.test.client;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;

import java.io.PrintStream;
import java.io.Reader;

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
