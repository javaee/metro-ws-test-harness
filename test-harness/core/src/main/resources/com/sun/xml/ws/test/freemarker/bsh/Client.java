package bsh;

<#list pImports as imp>
import ${imp};
</#list>

public class Client${stage} {

    ${util_bsh}

    String home = "${home}";
    // TODO
    List<URL> wsdlUrls = new ArrayList<URL>();
    static ${simpleName} ${serviceVarName} = new ${simpleName}();
    static ${portType} ${varName} = ${serviceName}.get${portType}Port();
    static URI ${varName}Address = null; // TODO: ${address}; //TODO

${client_setUp_script}

    public static void main(String[] args) throws Throwable {
        // bsh script START
        ${contents}
        // bsh script END

        System.out.println("= TEST PASSED: Client${stage}");
    }

    static void assertEquals(Object a, Object b) {
        if (a == null) {
            if (b != null) {
                throw new RuntimeException("ERROR: assertEquals: a is null, but b not!");
            }
        } else {
            if (!a.equals(b)) {
                throw new RuntimeException("ERROR: assertEquals: a NOT equal to b!");
            }
        }
        System.out.println("PASSED: assertEquals");
    }

    static void assertTrue(boolean condition) {
        if (!condition) {
            throw new RuntimeException("ERROR: assertTrue failed!");
        }
        System.out.println("PASSED: assertEquals");
    }

    static void fail() {
        fail("");
    }

    static void fail(String s) {
        throw new RuntimeException("ERROR: " + s);
    }
}
