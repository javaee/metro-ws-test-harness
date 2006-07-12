package jellytest;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.XMLOutput;

/**
 * Trying out jelly.
 */
public class Main {

    private static final JellyContext jContext = new JellyContext();
    
    public void testSunJaxws() throws Exception {
        Writer someWriter = new PrintWriter(System.out);
        XMLOutput output = XMLOutput.createXMLOutput( someWriter );
        SunJaxwsInfoBean infoBean = new SunJaxwsInfoBean();
        jContext.setVariable("data", infoBean);
        jContext.runScript("src/jellytest/sun-jaxws.jelly", output );
        output.flush();
    }
    
    public void testWebXML() throws Exception {
        Writer someWriter = new PrintWriter(System.out);
        XMLOutput output = XMLOutput.createXMLOutput( someWriter );
        WebXmlInfoBean infoBean = new WebXmlInfoBean();
        jContext.setVariable("data", infoBean);
        jContext.runScript("src/jellytest/web.jelly", output );
        output.flush();
    }

    public static void main(String[] args) {
        Main main = new Main();
        try {
            main.testWebXML();
            main.testSunJaxws();
        } catch (Exception e) {
            System.err.println("----------------");
            e.printStackTrace();
        }
    }
    
}
