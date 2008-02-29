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

import com.sun.xml.ws.test.util.FileUtil;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.RemotePropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfiguration;
import org.codehaus.cargo.util.CargoException;
import org.codehaus.cargo.util.DefaultFileHandler;

import java.io.File;
import java.io.FileWriter;

/**
 * @author Kohsuke Kawaguchi
 */
public class GlassfishStandaloneLocalConfiguration extends AbstractStandaloneLocalConfiguration {

    private static final ConfigurationCapability CAPABILITY =
        new GlassfishStandaloneLocalConfigurationCapability();

    /**
     *
     * @param home
     *      The work directory where files needed to run Glassfish will be created.
     *      Uses String, not File as per Cargo convention.
     */
    public GlassfishStandaloneLocalConfiguration(String home) {
        super(home);

        // default properties
        setProperty(RemotePropertySet.USERNAME, "admin");
        setProperty(RemotePropertySet.PASSWORD, "adminadmin");
        setProperty(GeneralPropertySet.HOSTNAME, "localhost");
        setProperty(GlassfishPropertySet.ADMIN_PORT, "4848");
        setProperty(GlassfishPropertySet.JMS_PORT, "7676");
        setProperty(GlassfishPropertySet.IIOP_PORT, "3700");
        setProperty(GlassfishPropertySet.HTTPS_PORT, "8181");
        setProperty(GlassfishPropertySet.IIOPS_PORT, "3820");
        setProperty(GlassfishPropertySet.IIOP_MUTUAL_AUTH_PORT, "3920");
        setProperty(GlassfishPropertySet.JMX_ADMIN_PORT, "8686");

        // ServletPropertySet.PORT default set to 8080 by the super class
    }

    public ConfigurationCapability getCapability() {
        return CAPABILITY;
    }

    /**
     * Returns the password file that contains admin's password.
     */
    File getPasswordFile() {
        return new File(getHome(), "password.properties");
    }

    /**
     * Creates a new domain and set up the workspace by invoking the "asadmin" command.
     */
    protected void doConfigure(LocalContainer container) throws Exception {
        DefaultFileHandler fileHandler = new DefaultFileHandler();
        fileHandler.delete(getHome());


        String password = getPropertyValue(RemotePropertySet.PASSWORD);
        if(password.length()<8)
            throw new CargoException("password needs to be 8 characters or longer");

        new File(getHome()).mkdirs();
        FileWriter w = new FileWriter(getPasswordFile());
        // somehow glassfish uses both. Brain-dead.
        w.write("AS_ADMIN_PASSWORD="+password+"\n");
        w.write("AS_ADMIN_ADMINPASSWORD="+password+"\n");
        w.close();


        ((GlassfishInstalledLocalContainer)container).invokeAsAdmin(
            false, "create-domain",
            "--interactive=false",
            "--adminport",
            getPropertyValue(GlassfishPropertySet.ADMIN_PORT),
            "--adminuser",
            getPropertyValue(RemotePropertySet.USERNAME),
            "--passwordfile",
            getPasswordFile().getAbsolutePath(),
            "--instanceport",
            getPropertyValue(ServletPropertySet.PORT),              
            "--domainproperties",
            getPropertyValueString(GlassfishPropertySet.JMS_PORT)+':'+
            getPropertyValueString(GlassfishPropertySet.IIOP_PORT)+':'+
            getPropertyValueString(GlassfishPropertySet.IIOPS_PORT)+':'+
            getPropertyValueString(GlassfishPropertySet.HTTPS_PORT)+':'+
            getPropertyValueString(GlassfishPropertySet.IIOP_MUTUAL_AUTH_PORT)+':'+
            getPropertyValueString(GlassfishPropertySet.JMX_ADMIN_PORT),
            "--template","wsit-test-domain.xml.template",
            "--domaindir",
            new File(getHome()).getAbsolutePath(),

            // it looks like domain name can be anything, but check with the dev
            "cargo-domain-" + getPropertyValue(ServletPropertySet.PORT));
        
        for (File src : com.sun.xml.ws.test.Main.containerClasspathPrefix) {
            File dest = new File(getHome(), "cargo-domain-" + getPropertyValue(ServletPropertySet.PORT) + "/lib/" + src.getName());
            FileUtil.copyFile(src,dest);
        }

        // schedule cargocpc for deployment
        File cpcWar = new File(getHome(), "cargocpc.war");
        getResourceUtils().copyResource(RESOURCE_PATH + "cargocpc.war",cpcWar);
        getDeployables().add(new WAR(cpcWar.getAbsolutePath()));
    }

    private String getPropertyValueString(String key) {
        String value = getPropertyValue(key);
        return key.substring("cargo.glassfish.".length())+'='+value;
    }

}
