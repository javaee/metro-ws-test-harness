
package jaxws.mtom.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the jaxws.mtom.client package. 
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

    private final static QName _DetailResponse_QNAME = new QName("http://example.org/mtom/data", "DetailResponse");
    private final static QName _Data_QNAME = new QName("http://example.org/mtom/data", "data");
    private final static QName _Detail_QNAME = new QName("http://example.org/mtom/data", "Detail");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: jaxws.mtom.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DetailType }
     * 
     */
    public DetailType createDetailType() {
        return new DetailType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DetailType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://example.org/mtom/data", name = "DetailResponse")
    public JAXBElement<DetailType> createDetailResponse(DetailType value) {
        return new JAXBElement<DetailType>(_DetailResponse_QNAME, DetailType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://example.org/mtom/data", name = "data")
    public JAXBElement<byte[]> createData(byte[] value) {
        return new JAXBElement<byte[]>(_Data_QNAME, byte[].class, null, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DetailType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://example.org/mtom/data", name = "Detail")
    public JAXBElement<DetailType> createDetail(DetailType value) {
        return new JAXBElement<DetailType>(_Detail_QNAME, DetailType.class, null, value);
    }

}
