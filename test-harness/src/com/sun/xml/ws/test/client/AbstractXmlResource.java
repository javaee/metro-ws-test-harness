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

    public Object asSOAP11Message() throws Exception {
        InterpreterEx i = new InterpreterEx(Thread.currentThread().getContextClassLoader());
        i.set("res",this);
        return i.eval(
            "factory = MessageFactory.newInstance();\n" +
            "message = factory.createMessage();\n" +
            "message.getSOAPPart().setContent(res.asStreamSource());\n" +
            "message.saveChanges();\n" +
            "return message;");
    }
}
