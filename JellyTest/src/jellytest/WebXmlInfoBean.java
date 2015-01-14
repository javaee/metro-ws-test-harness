package jellytest;

/**
 * This bean wraps the endpoint information. It is passed to
 * the Jelly script to create a web.xml file.
 *
 * The field names match the element names in the web.xml
 * template.
 */
public class WebXmlInfoBean {
    
    public String getDisplayName() {
        return "my display name";
    }
    
    public String getDescription() {
        return "my description";
    }
    
    public String getServletName() {
        return "some-servlet";
    }
    
    /*
     * Starting from wsdl, a service may have more than
     * one port. So the web.xml will have more than one
     * url mapping to the same jax-ws servlet. The
     * mappings in web.xml should match the endpoints
     * in sun-jaxws.xml.
     */
    public String [] getUrlPatterns() {
        String [] urlPatterns = { "foo", "bar" };
        return urlPatterns;
    }
    
}
