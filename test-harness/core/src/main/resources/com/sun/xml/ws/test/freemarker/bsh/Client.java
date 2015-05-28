package bsh;

<#list pImports as imp>
import ${imp};
</#list>
import java.net.URL;
import java.net.URI;

import static junit.framework.TestCase.*;
import static bsh.Util.*;


public class Client${stage} {

    static String home = "${home}";

    // TODO
    static List<URL> wsdlUrls = new ArrayList<URL>();

<#if serviceClassName??>
    static ${serviceClassName} ${serviceVarName} = new ${serviceClassName}();
</#if>
<#if portType??>
    static ${portType} ${varName} = ${serviceName}.${getPortMethod}();
</#if>
<#if varName??>
    static URI ${varName}Address = createUri("${address}");
</#if>

    // injected from bsh scripts
${client_setUp_script}

    static URI createUri(String s) {
        try{
            return new URI(s);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static void main(String[] args) throws Throwable {
        // bsh script START
        ${contents}
        // bsh script END

        System.out.println("= TEST PASSED: Client${stage}");
    }

}
