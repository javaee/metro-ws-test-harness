package jellytest;

/**
 * This bean wraps the endpoint information. It is passed to
 * the Jelly script to create a sun-jaxws.xml file. Really,
 * this bean just wraps one or more EndpointInfoBean
 * ojects since sun-jaxws.xml may have multiple endpoints.
 *
 * @see EndpointInfoBean
 */
public class SunJaxwsInfoBean {

    // everything hard coded just for testing
    public EndpointInfoBean [] getEndpointInfoBeans() {
        EndpointInfoBean bean1 = new EndpointInfoBean("Foo");
        EndpointInfoBean bean2 = new EndpointInfoBean("Bar");
        EndpointInfoBean [] endpointInfoBeans = { bean1, bean2 };
        return endpointInfoBeans;
    }
    
}
