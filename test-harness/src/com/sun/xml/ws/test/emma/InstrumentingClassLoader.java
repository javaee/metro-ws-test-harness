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
