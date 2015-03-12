package bsh;

import java.util.*;
import java.io.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.*;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


// freemarker template
public class Deploy${stage} {

    public static void main(String[] args) throws Throwable {
        Endpoint e = deploy();
        System.out.println("Endpoint [" + e + "] successfully deployed.");
        new EndpointStopper(${shutdownPort}, e);
    }

    static javax.xml.ws.Endpoint deploy() throws Exception {
        List<Source> metadata = new ArrayList<Source>();
<#list metadata_files as metadata_file>
        metadata.add(new StreamSource(new File("${metadata_file}")));
</#list>

// properties = {
<#list props?keys as key>
        //      ${key} : ${props[key]}
</#list>
        // }

        Map properties = new HashMap();
<#if portURI??>
        properties.put("javax.xml.ws.wsdl.port", new javax.xml.namespace.QName("${portURI}", "${portLOCAL}"));
</#if>
<#if svcURI??>
        properties.put("javax.xml.ws.wsdl.service", new javax.xml.namespace.QName("${svcURI}", "${svcLOCAL}"));
</#if>

        // testEndpoint.className = ${endpointImpl}
        Object endpointImpl = ${endpointImpl}.class.newInstance();

        javax.xml.ws.Endpoint endpoint = javax.xml.ws.Endpoint.create(endpointImpl);
        endpoint.setMetadata(metadata);
        endpoint.setProperties(properties);
        endpoint.publish("${endpointAddress}");
        return endpoint;
    }
}


class EndpointStopper {

    EndpointStopper(final int port, final Endpoint endpoint) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(port), 5);
        final ExecutorService threads = Executors.newFixedThreadPool(2);
        server.setExecutor(threads);
        server.start();

        HttpContext context = server.createContext("/stop");
        context.setHandler(new HttpHandler() {
            public void handle(HttpExchange msg) throws IOException {
                System.out.println("Shutting down the Endpoint");
                endpoint.stop();
                System.out.println("Endpoint is down");
                msg.sendResponseHeaders(200, 0);
                msg.close();
                server.stop(1);
                threads.shutdown();
            }
        });
    }
}