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

/**
 * Interface for Glassfish-specific properties.
 *
 */
public interface GlassfishPropertySet
{
    /**
     * The admin HTTP port that Glassfish will use.
     * Defaults to 4848.
     */
    String ADMIN_PORT = "cargo.glassfish.adminPort";

//
// these names are named to match asadmin --domainproperties option
//

    /**
     * JMS port. Defaults to 7676.
     */
    String JMS_PORT = "cargo.glassfish.jms.port";

    /**
     * IIOP port. Defaults to 3700.
     */
    String IIOP_PORT = "cargo.glassfish.orb.listener.port";

    /**
     * HTTPS port. Defaults to 8181.
     */
    String HTTPS_PORT = "cargo.glassfish.http.ssl.port";

    /**
     * IIOP+SSL port. Defaults to 3820.
     */
    String IIOPS_PORT = "cargo.glassfish.orb.ssl.port";

    /**
     * IIOP mutual authentication port. Defaults to 3920.
     */
    String IIOP_MUTUAL_AUTH_PORT = "cargo.glassfish.orb.mutualauth.port";

    /**
     * JMX admin port. Defaults to 8686.
     */
    String JMX_ADMIN_PORT = "cargo.glassfish.domain.jmxPort";
}
