/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Sun Microsystems, Inc. All rights reserved.
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

import com.sun.xml.ws.test.CodeGenerator;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.util.JavacTask;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Used to compile clients when there's no server to deploy.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClientCompileExecutor extends Executor {
    public ClientCompileExecutor(DeploymentContext context) {
        super("Compile clients "+context.descriptor.name, context);
    }

    public void runTest() throws Throwable {
        CodeGenerator.testStarting(context.workDir);
        File classDir = makeWorkDir("client-classes");

        // compile the generated source files to javac
        JavacTask javac = new JavacTask(context.descriptor.javacOptions);

        javac.setSourceDir(
            context.descriptor.common,
            new File(context.descriptor.home,"client")
        );
        javac.setDestdir(classDir);
        javac.setDebug(true);
        if(!context.wsimport.isNoop()) {
            // if we are just reusing the existing artifacts, no need to recompile.
            javac.execute();
            CodeGenerator.generateJavac(javac);
        }

        // load the generated classes and resources
        URL[] url = (context.getResources() == null)
                ? new URL[] {classDir.toURL()}
                : new URL[] {classDir.toURL(),context.getResources().toURL()};
        ClassLoader cl = new URLClassLoader( url, World.runtime.getClassLoader() );

        context.clientClassLoader = cl;
    }
}
