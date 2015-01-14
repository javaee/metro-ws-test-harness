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

package com.sun.xml.ws.test.container.cargo.gf;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.property.RemotePropertySet;
import org.codehaus.cargo.container.spi.deployer.AbstractLocalDeployer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class GlassfishInstalledLocalDeployer extends AbstractLocalDeployer {
    public GlassfishInstalledLocalDeployer(InstalledLocalContainer localContainer) {
        super(localContainer);
    }

    private GlassfishInstalledLocalContainer getLocalContainer() {
        return (GlassfishInstalledLocalContainer)super.getContainer();
    }
    private GlassfishStandaloneLocalConfiguration getConfiguration() {
        return (GlassfishStandaloneLocalConfiguration) getLocalContainer().getConfiguration();
    }

    public DeployerType getType() {
        return DeployerType.INSTALLED;
    }

    public void deploy(Deployable deployable) {
        doDeploy(deployable,false);
    }

    public void redeploy(Deployable deployable) {
        doDeploy(deployable,true);
    }

    private void doDeploy(Deployable deployable, boolean overwrite) {
        List<String> args = new ArrayList<String>();
        args.add("deploy");
        if(overwrite)
            args.add("--force");
        if(deployable instanceof WAR) {
            args.add("--contextroot");
            args.add(((WAR)deployable).getContext());
        }

        addConnectOptions(args);

        args.add(new File(deployable.getFile()).getAbsolutePath());

        getLocalContainer().invokeAsAdmin(false, args.toArray(new String[args.size()]));
    }

    public void undeploy(Deployable deployable) {
        List<String> args = new ArrayList<String>();
        args.add("undeploy");

        addConnectOptions(args);

        // not too sure how asadmin determines 'name'
        args.add(cutExtension(new File(deployable.getFile()).getName()));

        getLocalContainer().invokeAsAdmin(false, args.toArray(new String[args.size()]));
    }

    public void start(Deployable deployable) {
        // TODO
        super.start(deployable);
    }

    public void stop(Deployable deployable) {
        // TODO
        super.stop(deployable);
    }

    private String cutExtension(String name) {
        int idx = name.lastIndexOf('.');
        if(idx>=0)  return name.substring(0,idx);
        else        return name;
    }

    private void addConnectOptions(List<String> args) {
        args.add("--interactive=false");
        args.add("--port");
        args.add(getConfiguration().getPropertyValue(GlassfishPropertySet.ADMIN_PORT));
        args.add("--user");
        args.add(getConfiguration().getPropertyValue(RemotePropertySet.USERNAME));
        args.add("--passwordfile");
        args.add(getConfiguration().getPasswordFile().getAbsolutePath());
    }
}
