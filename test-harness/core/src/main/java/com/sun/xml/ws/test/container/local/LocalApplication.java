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

/*
 * LocalApplication.java
 *
 * Created on June 28, 2006, 10:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.xml.ws.test.container.local;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.model.TestEndpoint;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Application} implementation for {@link LocalApplicationContainer}.
 *
 * @author ken
 * @deprecated
 *      To be removed once in-vm transport becomes ready
 */
final class LocalApplication implements Application {

    private final @NotNull WAR war;

    /**
     * "local://path/to/exploded/dir" portion of the endpoint address.
     * Adding "?portName" makes it the full endpoint address.
     */
    private final @NotNull URI baseEndpointAddress;

    /** Creates a new instance of LocalApplication */
    LocalApplication(@NotNull WAR war, URI endpointAddress) {
        this.war = war;
        this.baseEndpointAddress = endpointAddress;
    }

    /**
     * Returns the actual endpoint address to which the given {@link TestEndpoint}
     * is deployed.
     */
    @NotNull
    public URI getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception {
        return new URI(baseEndpointAddress.toString() + '?' + endpoint.name); 
    }

    /**
     * Gets the WSDL of this service.
     *
     * <p>
     * This WSDL will be compiled to generate client artifacts during a test.
     */
    @NotNull
    public List<URL> getWSDL() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        for (File w : war.getWSDL()) {
            urls.add(w.toURL());
        }
        return urls;
    }

    /**
     * Removes this application from the container.
     */
    public void undeploy() throws Exception {
        // no-op. don't clean up artifacts since those are often necessary
        // to diagnose problems when the user is debugging a problem.

        // instead, clean up is done in LocalApplicationContainer.deploy()
    }
}
