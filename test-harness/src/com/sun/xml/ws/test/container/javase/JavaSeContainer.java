package com.sun.xml.ws.test.container.javase;

import com.sun.net.httpserver.HttpServer;

import com.sun.istack.NotNull;
import com.sun.xml.ws.test.container.AbstractApplicationContainer;
import com.sun.xml.ws.test.container.Application;
import com.sun.xml.ws.test.container.DeployedService;
import com.sun.xml.ws.test.container.WAR;
import com.sun.xml.ws.test.model.TestEndpoint;
import com.sun.xml.ws.test.tool.WsTool;
import com.sun.xml.ws.test.World;
import com.sun.xml.ws.test.client.InterpreterEx;
import java.io.FileWriter;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URLClassLoader;
import java.net.URL;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class JavaSeContainer extends AbstractApplicationContainer {
    
    private HttpServer appServer;
    private ExecutorService appExecutorService;

    private String sepChar;
    private File webappsDir;
    private File classesDir;
    private boolean stopped;
    private int port;
    
    public JavaSeContainer(WsTool wsimport, WsTool wsgen, int port) {
        super(wsimport,wsgen);
        //String homeDir = System.getProperty ("user.dir");
        String j2seServerDir = System.getProperty ("j2se.server.home");  // TODO -- Get location from main
        System.out.println("Server dir="+j2seServerDir);
        sepChar = System.getProperty ("file.separator");
        webappsDir = new File(j2seServerDir+sepChar+"webapps");
        classesDir = new File(webappsDir, "classes");
        System.out.println("webapps dir="+webappsDir.getAbsolutePath());
        this.port = port;
    }
    
    /*
    public static void main(String[] args) throws Exception {
        new JavaSeContainer();   
    }
    */

    public String getTransport() {
        return "http";
    }

    public void start() throws Exception {
        //appServer = HttpServer.create(new InetSocketAddress(port), 5);
        //appExecutorService = Executors.newFixedThreadPool(5);
        //appServer.setExecutor(appExecutorService);
        //appServer.start();
        //System.out.println("AppServer started");
    }
    
    public void shutdown() {
    }

   @NotNull
    public Application deploy(DeployedService service) throws Exception {
        String id = service.service.getGlobalUniqueName();
        WAR war = assembleWar(service);

        String endpointAddress = new String("http://localhost:" + port + "/" + id);

        if (service.service.isSTS)
            updateWsitClient(service, endpointAddress);

        for (File wsdl : war.getWSDL())
            patchWsdl(wsdl,endpointAddress);

        URLClassLoader serviceClassLoader = new URLClassLoader(
            new URL[]{new File(service.warDir,"WEB-INF/classes").toURL()},
            World.runtime.getClassLoader());
        InterpreterEx i = new InterpreterEx(serviceClassLoader);
        TestEndpoint testEndpoint = (TestEndpoint) service.service.endpoints.toArray()[0];
        Class endpointClass = serviceClassLoader.loadClass(testEndpoint.className);
        Object endpointInstance = endpointClass.newInstance();
        i.set("endpointAddress",endpointAddress);
        i.set("endpointInstance",endpointInstance);
        //FIXME
        
        Object server = i.eval("javax.xml.ws.Endpoint.publish(endpointAddress,endpointInstance);");
        //Object server = i.eval("System.out.println(\"BEANSHELL: \" + serviceClass );");

        return new JavaSeApplication(war,server,new URI(endpointAddress));
    }

    
     
 /*
    private void createEndpoint(Adapter adapter, File warDirFile)
    throws Exception {
        
        //String url = "http://localhost:8080/"+warDir+endpointInfo.getUrlPattern();
        //EndpointFactory.newInstance ().publish (url, endpointInfo.getImplementor());
        
        String urlPattern = adapter.urlPattern;
        if (urlPattern.endsWith("/*")) {
            urlPattern = urlPattern.substring(0, urlPattern.length() - 2);
        }
        String warDirName = warDirFile.getName();
        String contextRoot = "/"+warDirName+urlPattern;
        System.out.println("Context Root="+contextRoot);
        HttpContext context = appServer.createContext (contextRoot);
        
        // Creating endpoint from backdoor (and this publishes it, too)
        Endpoint endpoint = new EndpointImpl(adapter.getEndpoint(),context);
    }
 */

        public void updateWsitClient(DeployedService deployedService, String newLocation)throws Exception {
        File wsitClientFile = new File(deployedService.service.parent.resources,"wsit-client.xml");
        if (wsitClientFile.exists() ){
            SAXReader reader = new SAXReader();
            Document document = reader.read(wsitClientFile);
            Element root = document.getRootElement();
            Element policy = root.element("Policy");
            Element sts = policy.element("ExactlyOne").element("All").element("PreconfiguredSTS");

            Attribute  endpoint = sts.attribute("endpoint");
            
            newLocation = newLocation.replace('\\', '/');
            endpoint.setValue(newLocation);

            Attribute wsdlLoc = sts.attribute("wsdlLocation");
            wsdlLoc.setValue(deployedService.service.wsdl.wsdlFile.toURI().toString());

            XMLWriter writer = new XMLWriter(new FileWriter(wsitClientFile));
            writer.write( document );
            writer.close();

        } else {
            throw new RuntimeException("wsit-client.xml is absent. It is required. \n"+
                    "Please check " + deployedService.service.parent.resources );
        }
    }

    /**
     * Fix the address in the WSDL. to the local address.
     */

    private void patchWsdl(File wsdl, String endpointAddress) throws Exception {
        Document doc = new SAXReader().read(wsdl);
        List<Element> ports = doc.getRootElement().element("service").elements("port");

        for (Element port : ports) {
            String portName = port.attributeValue("name");

            Element address = (Element)port.elements().get(0);

            Attribute locationAttr = address.attribute("location");
            String newLocation =
                endpointAddress + "/" + portName;
                // "in-vm://" + id + "/?" + portName;
            newLocation = newLocation.replace('\\', '/');
            locationAttr.setValue(newLocation);
        }

        // save file
        FileOutputStream os = new FileOutputStream(wsdl);
        new XMLWriter(os).write(doc);
        os.close();
    }
}
