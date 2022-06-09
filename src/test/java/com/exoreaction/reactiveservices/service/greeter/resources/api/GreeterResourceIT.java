package com.exoreaction.reactiveservices.service.greeter.resources.api;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.reactiveservices.server.Server;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.Fields;
import org.junit.jupiter.api.*;

@Disabled
class GreeterResourceIT {

    static private Server server;
    static private HttpClient httpClient = new HttpClient();

    @BeforeAll
    public static void setUp() throws Exception {
        server = new Server(null, null);
        httpClient.start();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.close();
        httpClient.stop();

/* For debugging
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }
*/
    }


    @Test
    void get() throws Exception {
        httpClient.GET("http://localhost:8889/api/greeter").getContentAsString();
    }

    @Test
    void post() throws Exception {
        httpClient.FORM("http://localhost:8889/api/greeter", new Fields() {{
            put("greeting", "HelloWorld!");
        }}).getContentAsString();
    }

}