/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package com.thaiopensource.relaxng.jarv;

import java.io.IOException;

import com.thaiopensource.relaxng.parse.IllegalSchemaException;
import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.VerifierConfigurationException;
import org.iso_relax.verifier.VerifierFactory;
import org.relaxng.datatype.DatatypeLibraryFactory;
import org.relaxng.datatype.helpers.DatatypeLibraryLoader;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thaiopensource.relaxng.pattern.SchemaBuilderImpl;
import com.thaiopensource.relaxng.pattern.SchemaPatternBuilder;
import com.thaiopensource.relaxng.parse.Parseable;
import com.thaiopensource.relaxng.parse.compact.CompactParseable;
import com.thaiopensource.validate.IncorrectSchemaException;
import com.thaiopensource.xml.sax.DraconianErrorHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.thaiopensource.resolver.Resolver;
import com.thaiopensource.resolver.BasicResolver;

import com.thaiopensource.resolver.Input;

/**
 * {@link org.iso_relax.verifier.VerifierFactory} implementation
 * for RELAX NG Compact Syntax.
 * 
 * <p>
 * The reason why this class is in this package is to access
 * some of the package-private classes.
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class RelaxNgCompactSyntaxVerifierFactory extends VerifierFactory {
    private final DatatypeLibraryFactory dlf = new DatatypeLibraryLoader();
    private final ErrorHandler eh = new DraconianErrorHandler();

    public Schema compileSchema(InputSource is) throws VerifierConfigurationException, SAXException, IOException {
        SchemaPatternBuilder spb = new SchemaPatternBuilder();
        Input input = new Input();
        input.setByteStream(is.getByteStream());
        Parseable parseable = new CompactParseable(input, (Resolver)BasicResolver.getInstance(), eh);
        try {
                return new SchemaImpl(SchemaBuilderImpl.parse(parseable, eh, dlf, spb, false), spb);
        } catch (Exception ex) {
            throw new SAXException(ex.getMessage());
        }
    }

}
