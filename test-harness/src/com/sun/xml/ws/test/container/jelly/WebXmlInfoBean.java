package com.sun.xml.ws.test.container.jelly;

import com.sun.xml.ws.test.container.DeploymentContext;

import java.util.List;

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
    private final List<EndpointInfoBean> endpoints;

    /**
     * The constructor creates the fields queried by the Jelly script.
     * In many jax-ws web.xml files, the servlet name and display
     * name are the same. This same convention is followed here
     * for simplicity.
     * <p>
     * TODO: support for multiple ports. Currently hard-coding
     * url pattern and assuming only one port/service.
     */
    public WebXmlInfoBean(DeploymentContext context, List<EndpointInfoBean> endpoints) {
        description = context.descriptor.description;
        displayName = context.descriptor.name;
        servletName = context.descriptor.name;
        this.endpoints = endpoints;
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

    /**
     * Starting from wsdl, a service may have more than
     * one port. So the web.xml will have more than one
     * url mapping to the same jax-ws servlet. The
     * mappings in web.xml should match the endpoints
     * in sun-jaxws.xml.
     */
    public List<EndpointInfoBean> getEndpoints() {
        return endpoints;
    }

}
