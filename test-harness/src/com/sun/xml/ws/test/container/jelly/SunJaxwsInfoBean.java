/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

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
 * @see EndpointInfoBean
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
        i.set("service", war.service.service);
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
