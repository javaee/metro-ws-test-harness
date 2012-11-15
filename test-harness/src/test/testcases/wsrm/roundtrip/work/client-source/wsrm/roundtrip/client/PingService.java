
package wsrm.roundtrip.client;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.8-b13579
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "PingService", targetNamespace = "http://tempuri.org/", wsdlLocation = "file:/space/sources/ws-test-harness/trunk/test-harness/src/test/testcases/wsrm/roundtrip/work/services/server/war/WEB-INF/wsdl/EchoService.wsdl")
public class PingService
    extends Service
{

    private final static URL PINGSERVICE_WSDL_LOCATION;
    private final static WebServiceException PINGSERVICE_EXCEPTION;
    private final static QName PINGSERVICE_QNAME = new QName("http://tempuri.org/", "PingService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/space/sources/ws-test-harness/trunk/test-harness/src/test/testcases/wsrm/roundtrip/work/services/server/war/WEB-INF/wsdl/EchoService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        PINGSERVICE_WSDL_LOCATION = url;
        PINGSERVICE_EXCEPTION = e;
    }

    public PingService() {
        super(__getWsdlLocation(), PINGSERVICE_QNAME);
    }

    public PingService(WebServiceFeature... features) {
        super(__getWsdlLocation(), PINGSERVICE_QNAME, features);
    }

    public PingService(URL wsdlLocation) {
        super(wsdlLocation, PINGSERVICE_QNAME);
    }

    public PingService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, PINGSERVICE_QNAME, features);
    }

    public PingService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public PingService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns IPing
     */
    @WebEndpoint(name = "WSHttpBinding_IPing")
    public IPing getWSHttpBindingIPing() {
        return super.getPort(new QName("http://tempuri.org/", "WSHttpBinding_IPing"), IPing.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns IPing
     */
    @WebEndpoint(name = "WSHttpBinding_IPing")
    public IPing getWSHttpBindingIPing(WebServiceFeature... features) {
        return super.getPort(new QName("http://tempuri.org/", "WSHttpBinding_IPing"), IPing.class, features);
    }

    private static URL __getWsdlLocation() {
        if (PINGSERVICE_EXCEPTION!= null) {
            throw PINGSERVICE_EXCEPTION;
        }
        return PINGSERVICE_WSDL_LOCATION;
    }

}
