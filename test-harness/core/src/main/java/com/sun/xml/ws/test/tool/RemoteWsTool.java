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

package com.sun.xml.ws.test.tool;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launches {@code wsimport} as a separate process.
 *
 * @author Kohsuke Kawaguchi
 */
final class RemoteWsTool extends WsTool {

    /**
     * The path to the executable tool.bat or wsimport.sh
     */
    private final File executable;

    private String toolsExtraArgs = null;


    public RemoteWsTool(File executable, boolean dumpParameters, String toolsExtraArgs) {
        super(dumpParameters);
        this.executable = executable;
        this.toolsExtraArgs = toolsExtraArgs;
        if(!executable.exists())
            throw new IllegalArgumentException("Non-existent executable "+executable);
    }

    public void invoke(String... args) throws Exception {
        List<String> params = new ArrayList<String>();
        params.add(executable.getPath());

        // add http proxy properties as CLI arguments
        String proxyHost = System.getProperty("http.proxyHost");
        if (proxyHost != null) {
            params.add("-J-Dhttp.proxyHost="+proxyHost);
        }
        String proxyPort = System.getProperty("http.proxyPort");
        if (proxyPort != null) {
            params.add("-J-Dhttp.proxyPort="+proxyPort);
        }
        String nonProxyHosts = System.getProperty("http.nonProxyHosts");
        if (nonProxyHosts != null) {
            // if running in bash, value must be quoted
            if (!nonProxyHosts.startsWith("\"")) {
                nonProxyHosts = "\"" + nonProxyHosts + "\"";
            }
            params.add("-J-Dhttp.nonProxyHosts="+nonProxyHosts);
        }
        if (toolsExtraArgs != null) {
            System.err.println("adding extra tools args [" + toolsExtraArgs + "]");
            params.add(toolsExtraArgs);
        }


        params.addAll(Arrays.asList(args));

        if (dumpParams()) {
            dumpWsParams(params);
        }

        ProcessBuilder b = new ProcessBuilder(params);
        b.redirectErrorStream(true);
        Process proc = b.start();

        // copy the stream and wait for the process completion
        proc.getOutputStream().close();
        byte[] buf = new byte[8192];
        InputStream in = proc.getInputStream();
        int len;
        while((len=in.read(buf))>=0) {
            System.out.write(buf,0,len);
        }

        int exit = proc.waitFor();
        assertEquals(
            "wsimport reported exit code "+exit,
            0,exit);
    }

}
