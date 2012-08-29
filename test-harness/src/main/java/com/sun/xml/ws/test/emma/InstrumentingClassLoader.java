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

package com.sun.xml.ws.test.emma;

import com.vladium.emma.rt.IClassLoadHook;
import com.vladium.util.ByteArrayOStream;
import org.apache.tools.ant.loader.AntClassLoader2;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * {@link ClassLoader} that performs on-the-fly instrumentation.
 *
 * @author Kohsuke Kawaguchi
 */
final class InstrumentingClassLoader extends AntClassLoader2 {
    final IClassLoadHook transformer;

    public InstrumentingClassLoader(IClassLoadHook transformer) {
        this.transformer = transformer;
    }

    public final Class findClass(final String name) throws ClassNotFoundException {
        // find the class file image
        String classResource = name.replace('.', '/') + ".class";
        URL classURL = getResource(classResource);

        if (classURL == null)
            throw new ClassNotFoundException(name);

        try {
            byte[] image = readFully(classURL);

            ByteArrayOStream baos = new ByteArrayOStream(image.length);
            if (transformer.processClassDef(name, image, image.length, baos))
                image = baos.copyByteArray();

            return defineClass(name, image, 0, image.length);
        } catch (IOException ioe) {
            throw new Error(ioe);
        }
    }

    private byte[] readFully(URL resource) throws IOException {
        URLConnection con = resource.openConnection();
        InputStream in = con.getInputStream();
        try {
            int len = con.getContentLength();
            if(len!=-1) {
                byte[] buf = new byte[len];
                new DataInputStream(in).readFully(buf);
                return buf;
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                while((len=in.read(buf))>=0)
                    baos.write(buf,0,len);
                return baos.toByteArray();
            }
        } finally {
            in.close();
        }
    }
}
