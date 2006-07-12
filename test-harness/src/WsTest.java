/**
 * Convenient entry point that allows invocation like <tt>"java WsTest ..."</tt>,
 * assuming that everything is in the classpath.
 *
 * @author Kohsuke Kawaguchi
 */
public class WsTest {
    public static void main(String[] args) throws Exception {
        com.sun.xml.ws.test.Main.main(args);
    }
}
