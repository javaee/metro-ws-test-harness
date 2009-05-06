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

import com.sun.xml.ws.test.container.jelly.EndpointInfoBean;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.World;
import com.sun.istack.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Base implementation of {@link ApplicationContainer}.
 *
 * This implementation provides code for common tasks, such as assembling files
 * into a war, etc.
 *
 * @author Kohsuke Kawaguchi
 * @author Ken Hofsass
 */
public abstract class AbstractApplicationContainer implements ApplicationContainer {
    private final WsTool wsimport;
    private final WsTool wsgen;
    private final Set<String> unsupportedUses;
    private final boolean httpspi;

    protected AbstractApplicationContainer(WsTool wsimport, WsTool wsgen, boolean httpspi) {
        this.wsimport = wsimport;
        this.wsgen = wsgen;
        this.unsupportedUses = new HashSet<String>();
        this.httpspi = httpspi;
    }

    @NotNull
    public Set<String> getUnsupportedUses() {
        return unsupportedUses;
    }

    /**
     * Prepares an exploded war file image for this service.
     */
    protected final WAR assembleWar(DeployedService service) throws Exception {
        WAR war = new WAR(service);

        boolean fromJava = (service.service.wsdl==null);

        if(!fromJava)
            war.compileWSDL(wsimport);
        if(!isSkipMode())
            war.compileJavac();
        if(fromJava)
            war.generateWSDL(wsgen);

        if(!isSkipMode()) {
            List<EndpointInfoBean> endpoints = war.generateSunJaxWsXml();
            war.generateWebXml(endpoints, httpspi);

            // we only need this for Glassfish, but it's harmless to generate for other containers.
            // TODO: figure out how not to do this for other containers
            war.generateSunWebXml();


            PrintWriter w = new PrintWriter(new FileWriter(new File(war.root, "index.html")));
            w.println("<html><body>Deployed by the JAX-WS test harness</body></html>");
            w.close();
        }
        //Package Handler Configuration files
        war.copyHandlerChainFiles(service.service.getHandlerConfiguration());
        return war;
    }

    /**
     * Returns true if we are running with the "-skip" option,
     * where we shouldn't generate any artifacts and just pick up the results from the last run.
     */
    private boolean isSkipMode() {
        return wsgen.isNoop() && wsimport.isNoop();
    }

    /**
     * Prepares a fully packaged war file to the specified location.
     */
    protected final WAR createWARZip(DeployedService service, File archive) throws Exception {
        WAR assembly = assembleWar(service);

        // copy runtime classes into the classpath. this is slow.
        // isn't there a better way to do this?
        if(copyRuntimeLibraries()) {
            System.out.println("Copying runtime libraries");
            assembly.copyClasspath(World.runtime);
        }

        System.out.println("Assembling a war file");
        assembly.zipTo(archive);

        return assembly;
    }

    /**
     * Copy JAX-WS runtime code?
     */
    protected boolean copyRuntimeLibraries() {
        return false;
    }
}
