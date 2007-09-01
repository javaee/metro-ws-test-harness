package com.sun.xml.ws.test.client;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

/**
 * Partial default implementation.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractXmlResource implements XmlResource {
    public StreamSource asStreamSource() throws Exception {
        return new StreamSource(new StringReader(asString()));
    }
}
