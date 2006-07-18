package com.sun.xml.ws.test.container.jelly;

import com.sun.xml.ws.test.container.DeploymentContext;

/**
 * This bean wraps the endpoint information. It is passed to
 * the Jelly script to create a web.xml file.
 * <p>
 * The field names match the element names in the web.xml
 * template.
 */
public class WebXmlInfoBean {
    
    private final String description;
    private final String displayName;
    private final String servletName;
    private final String [] urlPatterns;
    
    /**
     * The constructor creates the fields queried by the Jelly script.
     * In many jax-ws web.xml files, the servlet name and display
     * name are the same. This same convention is followed here
     * for simplicity.
     * <p>
     * TODO: support for multiple ports. Currently hard-coding
     * url pattern and assuming only one port/service.
     */
    public WebXmlInfoBean(DeploymentContext context) {
        description = context.descriptor.description;
        displayName = context.descriptor.shortName;
        servletName = context.descriptor.shortName;
        urlPatterns = new String[1];
        urlPatterns[0] = "/pattern0";
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getServletName() {
        return servletName;
    }
    
    /*
     * Starting from wsdl, a service may have more than
     * one port. So the web.xml will have more than one
     * url mapping to the same jax-ws servlet. The
     * mappings in web.xml should match the endpoints
     * in sun-jaxws.xml.
     */
    public String [] getUrlPatterns() {
        return urlPatterns;
    }
    
}
