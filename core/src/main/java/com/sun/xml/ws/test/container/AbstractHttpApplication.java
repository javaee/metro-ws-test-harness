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
import com.sun.xml.ws.test.model.TestEndpoint;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * Partial {@link Application} implementation for web containers.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractHttpApplication implements Application {
    /**
     * URL to access this web application.
     */
    protected final URL warURL;

    protected final DeployedService service;

    protected AbstractHttpApplication(URL warURL, DeployedService service) {
        this.warURL = warURL;
        this.service = service;
    }

    @NotNull
    public URI getEndpointAddress(@NotNull TestEndpoint endpoint) throws Exception {
        return new URL(warURL,endpoint.name).toURI();
    }

    /**
     * When deployed to HTTP service, WSDL URL can be obtained by "?wsdl".
     */
    @NotNull
    public List<URL> getWSDL() throws Exception {
        List<URL> urls = new ArrayList<URL>();

        // TODO: if those endpoints point belong to the same service,
        // we end up returning multiple WSDLs that are really the same.
        // this should be harmless in terms of correctness, but
        // it's inefficient, as we'll do extra compilation.
        // can we avoid that?
        for (TestEndpoint ep : service.service.endpoints) {
            // somehow relative path computation doesn't work, so I rely on String concatanation. Ouch!
            urls.add(new URL(getEndpointAddress(ep)+"?wsdl"));
        }
        return urls;
    }

}
