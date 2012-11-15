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
import com.sun.xml.ws.test.model.TestService;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about running {@link TestService}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class DeployedService {

    /**
     * The {@link DeploymentContext} that owns this service.
     */
    public final @NotNull DeploymentContext parent;

    /**
     * Service that was deployed.
     */
    public final @NotNull TestService service;

    /**
     * {@link Application} that represents the currently deployed service
     * on the container.
     *
     * <p>
     * This field is set when a service is deployed.
     */
    public Application app;

    /**
     * Root of the working directory to store things related to this service.
     */
    public final @NotNull File workDir;

    /**
     * Directory to store a war file image.
     */
    public final @NotNull File warDir;

    /**
     * Classpaths to load client artifacts for this service.
     */
    public final List<URL> clientClasspaths = new ArrayList<URL>();

    /**
     * The classes that represents the generated <tt>Service</tt> classes.
     *
     * This field is populated when the service is deployed
     * and client artifacts are generated.
     *
     * In fromjava tests with multiple <tt>@WebService</tt>, you may actually
     * get multiple service classes for one deployed service (argh!)
     */
    public final List<Class> serviceClass = new ArrayList<Class>();

    /*package*/ DeployedService(DeploymentContext parent, TestService service) {
        this.parent = parent;
        this.service = service;

        // create work directory
        String rel = "services";
        if(service.name.length()>0)
            rel += '/' + service.name;
        this.workDir = new File(parent.workDir,rel);

        this.warDir = new File(workDir,"war");
    }

    /**
     * Creates working directory
     */
    /*package*/ void prepare() {
        warDir.mkdirs();
    }

    public File getResources() {
        return parent.getResources();
    }
}
