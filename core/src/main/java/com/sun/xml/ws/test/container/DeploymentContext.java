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

package com.sun.xml.ws.test.container;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.model.TestDescriptor;
import com.sun.xml.ws.test.model.TestService;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.util.FileUtil;
import junit.framework.TestCase;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * State of running {@link TestDescriptor} execution.
 *
 * {@link TestCase}s that work on the same {@link TestDescriptor}
 * shares this object to communicate information between them.
 *
 * @author Kohsuke Kawaguchi
 */
public class DeploymentContext {

    /**
     * The test descriptor that governs all the deployed services.
     */
    public final @NotNull TestDescriptor descriptor;

    /**
     * Container where services are deployed.
     */
    public final @NotNull ApplicationContainer container;

    /**
     * {@link WsTool} tool to be used.
     */
    public final @NotNull WsTool wsimport;

    /**
     * Which service is deployed where?
     */
    public final Map<TestService,DeployedService> services;

    /**
     * {@link ClassLoader} that loads all the generated client artifacts.
     * This is used to run client tests.
     */
    public ClassLoader clientClassLoader;

    /**
     * Work directory top.
     *
     * <p>
     * During test execution, this directory can be used as a temporary directory
     * to store various temporary artifacts.
     *
     * <p>
     * If you store something in the working directory, be sure to first create
     * a sub-directory, to avoid colliding with other parts of the harness
     * that uses the work directory.
     */
    public final File workDir;

    private File resources = null;

    public DeploymentContext(TestDescriptor descriptor, ApplicationContainer container, WsTool wsimport) {
        this.descriptor = descriptor;
        this.container = container;
        this.wsimport = wsimport;

        // create workspace
        this.workDir = new File(descriptor.home,"work");

        // create DeployedService objects
        Map<TestService,DeployedService> services = new HashMap<TestService, DeployedService>();
        for (TestService svc : descriptor.services) {
            services.put(svc, new DeployedService(this,svc));
        }
        this.services = Collections.unmodifiableMap(services);
    }

    /**
     * Creates working directories.
     *
     * @param clean
     *      if true, all the previous left-over files in the working directory
     *      will be deleted.
     */
    public void prepare(boolean clean) {
        if(clean) {
            FileUtil.deleteRecursive(workDir);
        }
        workDir.mkdirs();
        if (descriptor.resources != null) {
            resources = new File(workDir, "resources");
            FileUtil.copyDir(descriptor.resources, resources, null);
        }

        for (DeployedService ds : services.values()) {
            ds.prepare();
        }
    }

    public File getResources() {
        return resources;
    }
}
