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

package com.sun.xml.ws.test.client;

import bsh.Interpreter;
import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.model.TestClient;
import com.sun.xml.ws.test.model.TestDescriptor;
import junit.framework.Assert;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;

/**
 * Client test script will be executed as if it's a method
 * on a sub-class of this class. IOW,
 *
 * <pre>
 * class Dummy extends {@link ScriptBaseClass} {
 *   void scriptMethod() {
 *     ... contents of the script ...
 *   }
 * }
 * </pre>
 *
 * <p>
 * Therefore all the public methods and fields are visible
 * to the script. This is a convenient place to define helper
 * convenience methods for scripts to use.
 *
 * <p>
 * Functions defined in <tt>util.bsh</tt> serves the same role.
 *
 * @author Kohsuke Kawaguchi
 */
public class ScriptBaseClass extends Assert {

    private final DeploymentContext context;
    private final TestClient client;
    private final Interpreter engine;

    public ScriptBaseClass(DeploymentContext context, Interpreter engine, TestClient client) {
        this.context = context;
        this.client = client;
        this.engine = engine;
    }

    /**
     * Loads a resource.
     *
     * @param name
     *      The resource name like "test.png" or "subdir1/subdir2/foo.xml"
     *
     * @see TestDescriptor#resources
     */
    public File resource(@NotNull String name) {
        return new File(context.getResources(),name);
    }

    public Source makeSaxSource(String msg) {
        ByteArrayInputStream saxinputStream = new ByteArrayInputStream(msg.getBytes());
        return new SAXSource(new InputSource(saxinputStream));
    }

    public static Source makeStreamSource(String msg) {
        return new StreamSource(new ByteArrayInputStream(msg.getBytes()));
    }

    public static Collection<Source> makeMsgSource(String msg) throws IOException, ParserConfigurationException, SAXException {
        Collection<Source> sourceList = new ArrayList<Source>();

        byte[] bytes = msg.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ByteArrayInputStream saxinputStream = new ByteArrayInputStream(bytes);
        InputSource inputSource = new InputSource(saxinputStream);

        ByteArrayInputStream sinputStream = new ByteArrayInputStream(bytes);

        DOMSource domSource = new DOMSource(createDOMNode(inputStream));
        sourceList.add(domSource);
        SAXSource saxSource = new SAXSource(inputSource);
        sourceList.add(saxSource);
        StreamSource streamSource = new StreamSource(sinputStream);
        sourceList.add(streamSource);

        return sourceList;
    }

    public static Source makeDOMSource(String msg) throws IOException, ParserConfigurationException, SAXException {
        byte[] bytes = msg.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        return new DOMSource(createDOMNode(inputStream));
    }

    public static Node createDOMNode(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(inputStream);
    }

    public String sourceToXMLString(Source result) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        OutputStream out = new ByteArrayOutputStream();
        StreamResult streamResult = new StreamResult();
        streamResult.setOutputStream(out);
        transformer.transform(result, streamResult);

        return streamResult.getOutputStream().toString();
    }
    
    // more to come
}
