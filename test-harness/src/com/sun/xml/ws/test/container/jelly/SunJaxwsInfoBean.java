package com.sun.xml.ws.test.container.jelly;

import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.client.InterpreterEx;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.util.FileUtil;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * This bean wraps the endpoint information. It is passed to
 * the Jelly script to create a sun-jaxws.xml file. Really,
 * this bean just wraps one or more EndpointInfoBean
 * ojects since sun-jaxws.xml may have multiple endpoints.
 *
 * @see com.sun.xml.ws.test.container.jelly.EndpointInfoBean
 */
public class SunJaxwsInfoBean {

    private final List<EndpointInfoBean> endpointInfoBeans = new ArrayList<EndpointInfoBean>();

    /**
     * The constructor creates the fields queried by the Jelly script.
     */
    public SunJaxwsInfoBean(WAR war)
        throws Exception {

        ClassLoader loader = new URLClassLoader(
            new URL[]{war.classDir.toURL()},
            World.runtime.getClassLoader());
        String[] classNames = FileUtil.getClassFileNames(war.classDir);

        InterpreterEx i = new InterpreterEx(loader);
        i.getNameSpace().importStatic(EndpointInfoBean.class);
        i.set("fromWsdl", war.service.service.wsdl != null);
        i.set("classNames", classNames);
        i.set("loader", loader);
        i.set("endpoints",war.service.service.endpoints);
        i.set("beans", endpointInfoBeans);
        i.eval(new InputStreamReader(getClass().getResourceAsStream("sun-jaxws.bsh")));


        //Class wscAnnotationClass =
        //    loader.loadClass("javax.xml.ws.WebServiceClient");
        //Method wscNameMethod = wscAnnotationClass.getMethod(
        //    "name", new Class[0]);
        //Method wscTargetNamespaceMethod = wscAnnotationClass.getMethod(
        //    "targetNamespace", new Class[0]);
        //Method wscWsdlLocationMethod = wscAnnotationClass.getMethod(
        //    "wsdlLocation", new Class[0]);
        //
        //Class wsAnnotationClass =
        //    loader.loadClass("javax.jws.WebService");
        //Method wsEndpointInterfaceMethod = wsAnnotationClass.getMethod(
        //    "endpointInterface", new Class[0]);
        //
        //Class weAnnotationClass =
        //    loader.loadClass("javax.xml.ws.WebEndpoint");
        //Method weNameMethod = weAnnotationClass.getMethod(
        //    "name", new Class[0]);
        //
        //String serviceName = "";
        //String implClass = ""; // need to map these to port names somehow
        //String targetNamespace = "";
        //String wsdlLocation = "";
        //List<String> portNames = new ArrayList<String>();
        //
        //for (String className : classNames) {
        //    Class clazz = loader.loadClass(className);
        //    Object annotation = clazz.getAnnotation(wscAnnotationClass);
        //    if (annotation != null) {
        //        serviceName = (String) wscNameMethod.invoke(
        //            annotation, new Object [0]);
        //        targetNamespace = (String) wscTargetNamespaceMethod.invoke(
        //            annotation, new Object [0]);
        //        wsdlLocation = (String) wscWsdlLocationMethod.invoke(
        //            annotation, new Object [0]);
        //        //hacky
        //        wsdlLocation = wsdlLocation.replace('\\', '/');
        //        wsdlLocation = "WEB-INF/wsdl/" +
        //            wsdlLocation.substring(
        //            wsdlLocation.lastIndexOf("/") + 1,
        //            wsdlLocation.length());
        //        for (Method method : clazz.getMethods()) {
        //            annotation = method.getAnnotation(weAnnotationClass);
        //            if (annotation != null) {
        //                String name = (String) weNameMethod.invoke(
        //                    annotation, new Object[0]);
        //                if (name != null && !name.equals("")) {
        //                    portNames.add(name);
        //                }
        //            }
        //        }
        //    }
        //
        //    annotation = clazz.getAnnotation(wsAnnotationClass);
        //    if (annotation != null) {
        //        String sei = (String) wsEndpointInterfaceMethod.invoke(
        //            annotation, new Object[0]);
        //        if (sei != null && !sei.equals("")) {
        //            implClass = clazz.getName();
        //        }
        //    }
        //}
        //
        //// create endpoint info beans
        //int urlPatternNum = 0;
        //for (String portName : portNames) {
        //    EndpointInfoBean bean = new EndpointInfoBean(
        //        serviceName,
        //        implClass,
        //        wsdlLocation,
        //        new QName(targetNamespace, serviceName),
        //        new QName(targetNamespace, portName),
        //        "binding",
        //        "/pattern" + urlPatternNum);
        //    urlPatternNum++;
        //    endpointInfoBeans.add(bean);
        //}
    }

    public List<EndpointInfoBean> getEndpointInfoBeans() {
        return endpointInfoBeans;
    }

}
