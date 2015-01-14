/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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
package com.sun.xml.ws.test.container.cargo;

import com.sun.xml.ws.test.tool.WsTool;
import java.io.IOException;
import java.net.ServerSocket;
import org.codehaus.cargo.container.LocalContainer;

import java.net.URL;
import java.util.Random;

/**
 * Common implementation of {@link EmbeddedCargoApplicationContainer}
 * and {@link InstalledCargoApplicationContainer}.
 *
 * This class also assumes that the launched container can be accessible
 * by "localhost".
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractRunnableCargoContainer<C extends LocalContainer> extends AbstractCargoContainer<C> {

    protected final int httpPort;

    protected AbstractRunnableCargoContainer(WsTool wsimport, WsTool wsgen, int port, boolean httpspi) {
        super(wsimport, wsgen, httpspi);
        httpPort = port < 0 ? getFreePort() : port;
    }

    public void start() throws Exception {
        System.out.println("Starting "+container.getId());
        container.start();
    }

    public void shutdown() throws Exception {
        System.out.println("Stopping "+container.getId());
        container.stop();
    }

    protected URL getServiceUrl(String contextPath) throws Exception {
        return new URL(Boolean.getBoolean("harness.useSSL") ? "https" : "http", "localhost", httpPort, "/" + contextPath + "/");
    }

    private static int findFreePort(int x) {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(x);
            return ss.getLocalPort();
        } catch (IOException ioe) {
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
        return -1;
    }

    public static int getFreePort() {
        // set TCP port to somewhere between 20000-60000
        for (int i = 0; i < 10; i++) {
            int p = findFreePort(new Random().nextInt(40000) + 20000);
            if (p > 0) {
                return p;
            }
        }
        System.err.println("Couldn't find free port");
        return -1;
    }


}
