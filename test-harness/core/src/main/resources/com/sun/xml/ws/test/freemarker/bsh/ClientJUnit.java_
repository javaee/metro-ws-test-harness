package bsh;

import static junit.framework.TestCase.*;

public class ClientJUnit${stage} {

<#list methods as m>
    private static void test_${m}() throws Exception {
        ${className} test =
            new ${className}(${constructorArg});

         try {
<#if injectedProperties??>
<#list injectedProperties?keys as key>
            System.setProperty("${key}", "${injectedProperties[key]}");
</#list>
</#if>
            invoke(test, "setUp");
            test.${m}();

            System.out.println("= TEST PASSED: ClientJUnit${stage} / ${m}");
        } finally{
            invoke(test, "tearDown");
        }
    }
</#list>

    public static void main(String[] args) throws Throwable {
<#list methods as m>
        test_${m}();
</#list>
    }

    static void invoke(Object target, String method) throws java.lang.reflect.InvocationTargetException,
                                                            IllegalAccessException {
        try {
            java.lang.reflect.Method m=target.getClass().getDeclaredMethod(method, new Class<?>[0]);
            m.setAccessible(true);
            m.invoke(target, new Object[0]);
        } catch (NoSuchMethodException e) {
        }
    }


}
