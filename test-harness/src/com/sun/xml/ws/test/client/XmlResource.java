package com.sun.xml.ws.test.client;

import javax.xml.transform.stream.StreamSource;

/**
 * XML Resource to be injected to the client.
 * Defiend by &lt;xml-resource> element in test descriptor. 
 *
 * @author Kohsuke Kawaguchi
 */
public interface XmlResource {
    StreamSource asStreamSource() throws Exception;
    String asString() throws Exception;
}
