
package fromjava_handlers.server.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.8-b13579
 * Generated source version: 2.2.8
 * 
 */
@XmlRootElement(name = "AddNumbersException", namespace = "http://server.fromjava_handlers/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AddNumbersException", namespace = "http://server.fromjava_handlers/", propOrder = {
    "detail",
    "message"
})
public class AddNumbersExceptionBean {

    private String detail;
    private String message;

    /**
     * 
     * @return
     *     returns String
     */
    public String getDetail() {
        return this.detail;
    }

    /**
     * 
     * @param detail
     *     the value for the detail property
     */
    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * 
     * @return
     *     returns String
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * 
     * @param message
     *     the value for the message property
     */
    public void setMessage(String message) {
        this.message = message;
    }

}
