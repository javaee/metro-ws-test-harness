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

package com.sun.xml.ws.test.model;

import com.sun.istack.Nullable;

import java.io.File;
import java.util.List;

/**
 * @author Bhakti Mehta
 */
public class WSDL {

    //Optional WSDL file that describes this service.
    @Nullable
    public final File wsdlFile;

    //Optional imported wsdls that are imported by the primary wsdl (wsdlFile)
    @Nullable
    public final List<File> importedWsdls;

    //Optional schema files that are imported by the wsdl
    @Nullable
    public final List<File> schemas;

    //Optional relative location of the WSDL (under WEB-INF/wsdl tree)
    @Nullable
    public final String relativeLocation;


    public WSDL(File wsdlFile ,List<File> importedWsdls,List<File> schemafiles, String relativeLocation) {
        this.wsdlFile = wsdlFile;
        this.importedWsdls = importedWsdls;
        this.schemas = schemafiles;
        this.relativeLocation = relativeLocation;
    }

}
