/*
 * CustomizationBean.java
 *
 * Created on July 14, 2006, 9:59 AM
 */

package com.sun.xml.ws.test.container.local.jelly;

/**
 * @author WS Test Harness Team
 */
public class CustomizationBean {
    
    private String packageName;
    private String wsdlFileName;
    private String targetNameSpace;

    public CustomizationBean(String packageName, String wsdlFileName, String targetNameSpace) {
        this.packageName = packageName;
        this.wsdlFileName = wsdlFileName;
        this.targetNameSpace = targetNameSpace;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public String getWsdlFileName() {
        return wsdlFileName;
    }
    
    public String targetNameSpace() {
        return targetNameSpace;
    }
}
