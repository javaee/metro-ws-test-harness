
package wsrm.roundtrip.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the wsrm.roundtrip.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _PingRequestBodyTypeText_QNAME = new QName("http://tempuri.org/", "Text");
    private final static QName _PingRequestBodyTypeSequence_QNAME = new QName("http://tempuri.org/", "Sequence");
    private final static QName _EchoString_QNAME = new QName("http://tempuri.org/", "echoString");
    private final static QName _PingResponseBodyType_QNAME = new QName("http://tempuri.org/", "PingResponse.BodyType");
    private final static QName _PingRequestBodyType_QNAME = new QName("http://tempuri.org/", "PingRequest.BodyType");
    private final static QName _EchoStringResponse_QNAME = new QName("http://tempuri.org/", "echoStringResponse");
    private final static QName _PingResponseBodyTypeEchoStringReturn_QNAME = new QName("http://tempuri.org/", "EchoStringReturn");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: wsrm.roundtrip.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PingResponseBodyType }
     * 
     */
    public PingResponseBodyType createPingResponseBodyType() {
        return new PingResponseBodyType();
    }

    /**
     * Create an instance of {@link PingRequestBodyType }
     * 
     */
    public PingRequestBodyType createPingRequestBodyType() {
        return new PingRequestBodyType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "Text", scope = PingRequestBodyType.class)
    public JAXBElement<String> createPingRequestBodyTypeText(String value) {
        return new JAXBElement<String>(_PingRequestBodyTypeText_QNAME, String.class, PingRequestBodyType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "Sequence", scope = PingRequestBodyType.class)
    public JAXBElement<String> createPingRequestBodyTypeSequence(String value) {
        return new JAXBElement<String>(_PingRequestBodyTypeSequence_QNAME, String.class, PingRequestBodyType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PingRequestBodyType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "echoString")
    public JAXBElement<PingRequestBodyType> createEchoString(PingRequestBodyType value) {
        return new JAXBElement<PingRequestBodyType>(_EchoString_QNAME, PingRequestBodyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PingResponseBodyType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "PingResponse.BodyType")
    public JAXBElement<PingResponseBodyType> createPingResponseBodyType(PingResponseBodyType value) {
        return new JAXBElement<PingResponseBodyType>(_PingResponseBodyType_QNAME, PingResponseBodyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PingRequestBodyType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "PingRequest.BodyType")
    public JAXBElement<PingRequestBodyType> createPingRequestBodyType(PingRequestBodyType value) {
        return new JAXBElement<PingRequestBodyType>(_PingRequestBodyType_QNAME, PingRequestBodyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PingResponseBodyType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "echoStringResponse")
    public JAXBElement<PingResponseBodyType> createEchoStringResponse(PingResponseBodyType value) {
        return new JAXBElement<PingResponseBodyType>(_EchoStringResponse_QNAME, PingResponseBodyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "EchoStringReturn", scope = PingResponseBodyType.class)
    public JAXBElement<String> createPingResponseBodyTypeEchoStringReturn(String value) {
        return new JAXBElement<String>(_PingResponseBodyTypeEchoStringReturn_QNAME, String.class, PingResponseBodyType.class, value);
    }

}
