package junit.framework;

/**
 * Created by miran on 02/03/15.
 */
public class TestCase {

    public TestCase() {}
    public TestCase(String name) {}

    public static void assertEquals(String msg, Object a, Object b) {
        if (a == null) {
            if (b != null) {
                fail(msg + " assertEquals: a is null, but b not!");
            }
        } else {
            if (!a.equals(b))
                fail(msg + " assertEquals: a NOT equal to b!");
        }
        pass("assertEquals");
    }

    public static void assertEquals(Object a, Object b) {
        assertEquals("", a, b);
    }

    public static void assertFalse(boolean condition) {
        if (condition)
            fail("assertFalse failed!");
        pass("assertFalse");
    }

    public static void assertNull(Object nullObject) {
        if (nullObject != null)
            fail("assertNull failed!");
        pass("assertNull");
    }

    public static void assertNotNull(Object object) {
        if (object == null)
            fail("assertNotNull failed!");
        pass("assertNotNull");
    }

    public static void assertTrue(boolean condition) {
        if (!condition)
            fail("assertTrue failed!");
        pass("assertTrue");
    }

    public static void fail() {
        fail("");
    }

    public static void fail(String s) {
        throw new RuntimeException("ERROR: " + s);
    }

    public static void pass(String msg) {
        System.out.println("PASSED: " + msg);
    }

}
