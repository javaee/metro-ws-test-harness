package com.sun.xml.ws.test.client;

import javax.xml.transform.stream.StreamSource;

/**
 * XML Resource to be injected to the client.
 * Defiend by &lt;xml-resource> element in test descriptor. 
 *
 * @author Kohsuke Kawaguchi
 */
public interface XmlResource {
    /**
     * Returns this XML as a {@link StreamSource}.
     */
    StreamSource asStreamSource() throws Exception;
    /**
     * Returns this XML as a String literal.
     */
    String asString() throws Exception;
    /**
     * Reads it as SAAJ SOAPMessage in SOAP 1.1 and return it.
     */
    Object asSOAP11Message() throws Exception;
}
