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

package com.sun.xml.ws.test;

import com.sun.xml.ws.test.emma.Emma;
import org.apache.tools.ant.Project;
import org.codehaus.classworlds.ClassRealm;

/**
 * "Global variables" for the test harness. Use with caution.
 *
 * This class includes pointers to
 * various {@link ClassRealm}s that represent compartments inside the VM.
 *
 * <p>
 * The followings are the key realms:
 *
 * <ol>
 * <li>"harness" realm that loads all the test harness code,
 *     including lots of 3rd party jars.
 * <li>"runtime" realm that loads the classes that the client script will use
 *     to execute tests.
 * <li>"wsimport" realm that loads the tool/wsgen tools, if we invoke it
 *     within the same VM. Otherwise this realm is empty.
 *
 * <p>
 * Realms are created when {@link World} is created, but they are filled in
 * from {@link Main}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class World {
    /**
     * Whenever we need a {@link Project} to use Ant tasks,
     * we can use this shared instance.
     */
    public static final Project project = new Project();

    static {
        project.init();
    }

    /**
     * Loads JAX-WS runtime classes.
     *
     * This realm is also used to load the embedded application container,
     * so that we don't have to package JAX-WS runtime into the war file.
     */
    public static final Realm runtime = new Realm("runtime",null);
    public static final Realm tool    = new Realm("tool",   runtime);

    /**
     * @see Main#debug
     */
    public static boolean debug = false;

    /**
     * @see Main#emma
     */
    public static Emma emma = null;
}
