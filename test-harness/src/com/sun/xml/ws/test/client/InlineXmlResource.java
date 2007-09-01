package com.sun.xml.ws.test.client;

import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import java.io.StringWriter;

/**
 * Resource XML defined inline.
 *
 * @author Kohsuke Kawaguchi
 */
public class InlineXmlResource extends AbstractXmlResource {
    private final Element root;

    public InlineXmlResource(Element root) {
        this.root = root;
    }

    public String asString() throws Exception {
        StringWriter sw = new StringWriter();
        new XMLWriter(sw).write(root);
        return sw.toString();
    }
}
