/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Sun Microsystems, Inc. All rights reserved.
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

import com.sun.xml.ws.test.World;
import junit.framework.Assert;

import java.io.File;
import java.util.List;

/**
 * Interface to the <tt>wsimport</tt> or <tt>wsgen</tt> command-line tool.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WsTool extends Assert {

    private boolean dumpParameters;

    protected WsTool(boolean dumpParameters) {
        this.dumpParameters = dumpParameters;
    }

    /**
     * Invokes wsimport with the given arguments.
     *
     * @throws Exception
     *      if the compilation fails.
     *      alternatively, JUnit {@link Assert}ions can be used
     *      to detect error conditions.
     */
    public abstract void invoke(String... args) throws Exception;

    /**
     * Returns true if the '-skip' mode is on and
     * the tool invocation is skipped.
     */
    public boolean isNoop() {
        return this==NOOP;
    }

    /**
     * Determines which compiler to use.
     *
     * @param externalWsImport
     *      null to run {@link com.sun.xml.ws.test.tool.WsTool} from {@link com.sun.xml.ws.test.World#tool}.
     *      Otherwise this file will be the path to the script.
     * @param extraWsToolsArgs
     */
    public static WsTool createWsImport(File externalWsImport, boolean dumpParameters, String extraWsToolsArgs) {
        return createTool(externalWsImport, "com.sun.tools.ws.WsImport", dumpParameters, extraWsToolsArgs);
    }

    /**
     * Determines which wsgen to use.
     *
     * @param externalWsGen
     *      null to run {@link com.sun.xml.ws.test.tool.WsTool} from {@link com.sun.xml.ws.test.World#tool}.
     *      Otherwise this file will be the path to the script.
     * @param extraWsToolsArgs
     */
    public static WsTool createWsGen(File externalWsGen, boolean dumpParameters, String extraWsToolsArgs) {
        return createTool(externalWsGen,"com.sun.tools.ws.WsGen", dumpParameters, extraWsToolsArgs);
    }

    private static WsTool createTool(File externalExecutable, String className, boolean dumpParameters, String extraWsToolsArgs) {
        if(externalExecutable !=null) {
            return new RemoteWsTool(externalExecutable, dumpParameters, extraWsToolsArgs);
        } else {
            return new LocalWsTool(className, dumpParameters);
        }
    }

    /**
     * {@link WsTool} that does nothing.
     *
     * <p>
     * This assumes that files that are supposed to be generated
     * are already generated.
     */
    public static WsTool NOOP = new WsTool(false) {
        public void invoke(String... args) {
        }
    };

    protected void dumpWsParams(List<String> params) {
        System.err.println("\n\nINVOKING WS Tool:\n");
        for(int i = 0; i < params.size(); i++) {
            System.err.print(i == 0? " " : "     ");
            System.err.print(params.get(i));
            System.err.println(i + 1 < params.size()? " \\" : "\n");
        }
    }

    protected boolean dumpParams() {
        return dumpParameters;
    }

}
