package jellytest;

/**
 * This bean wraps the endpoint information. It is passed to
 * the Jelly script to create a sun-jaxws.xml file.
 *
 * @see SunJaxwsInfoBean
 */
public class EndpointInfoBean {

    String name;
    String implementation;
    String wsdl;
    String service;
    String port;
    String binding;
    String urlPattern;
    
    public EndpointInfoBean(String name) {
        this.name = name;
        implementation = "com.example." + name + "_Impl";
        wsdl = "WEB-INF/wsdl/example.wsdl";
        service = "{urn:test}ExampleService";
        port = "{urn:test}" + name + "Port";
        binding = "http://www.w3.org/2003/05/soap/bindings/HTTP/";
        urlPattern = "/" + name.toLowerCase();
    }

    public String getName() {
        return name;
    }
    
    public String getImplementation() {
        return implementation;
    }
    
    public String getWsdl() {
        return wsdl;
    }
    
    public String getService() {
        return service;
    }
    
    public String getPort() {
        return port;
    }
    
    public String getBinding() {
        return binding;
    }
    
    public String getUrlPatterne() {
        return urlPattern;
    }
    
}
