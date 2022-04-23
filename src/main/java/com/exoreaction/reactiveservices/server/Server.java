package com.exoreaction.reactiveservices.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.reactiveservices.jsonapi.Attributes;
import com.exoreaction.reactiveservices.jsonapi.JsonApiRels;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.ResourceObjects;
import com.exoreaction.reactiveservices.jetty.server.JettyConnectorThreadPool;
import com.exoreaction.reactiveservices.rest.RestClient;
import com.exoreaction.reactiveservices.service.configuration.Configuration;
import com.exoreaction.reactiveservices.service.configuration.StandardConfiguration;
import io.dropwizard.metrics.jetty11.InstrumentedHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

public class Server
        implements Closeable {
    private Configuration configuration;
    private org.eclipse.jetty.server.Server server;
    private List<ResourceObject> services = new CopyOnWriteArrayList<>();

    public Server() throws Exception {
        this.configuration = configuration();

        services.add(new ResourceObject.Builder("server")
                        .attributes(new Attributes.Builder()
                                .attribute("jetty.version", Jetty.VERSION)
                                .build()).build());
        MetricRegistry metricRegistry = metrics();

        webServer(metricRegistry);
    }


    public void addService(ResourceObject serviceResource) {
        services.add(serviceResource);
    }

    public ResourceDocument getServerDocument()
    {
        ResourceDocument serverDocument = new ResourceDocument.Builder()
                .links(new Links.Builder().link(JsonApiRels.self, "http://localhost:8080/").build())
                .data(services.stream().collect(ResourceObjects.toResourceObjects()))
                .build();
        return serverDocument;
    }

    private Configuration configuration() throws Exception {
        Configuration.Builder builder = new Configuration.Builder();

        // Load defaults
        builder.addYaml(Configuration.class.getResourceAsStream("/server.yaml"));

        // Load overrides
        Configuration partialConfig = builder.build();
        StandardConfiguration standardConfiguration = new StandardConfiguration(partialConfig);
        builder = partialConfig.asBuilder();
        File overridesYamlFile = new File(standardConfiguration.home(), "conf/server_overrides.yaml");
        if (overridesYamlFile.exists()) {
            FileInputStream overridesYamlStream = new FileInputStream(overridesYamlFile);
            builder.addYaml(overridesYamlStream);
        }

        // Load user
        File userYamlFile = new File(System.getProperty("user.home"), "reactive/server.yaml");
        if (userYamlFile.exists()) {
            FileInputStream userYamlStream = new FileInputStream(userYamlFile);
            builder.addYaml(userYamlStream);
        }

        return builder.build();
    }

    private MetricRegistry metrics() {
        MetricRegistry metricRegistry = new MetricRegistry();

        // Setup Jvm mem metric
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOperatingSystemMXBean = (com.sun.management
                .OperatingSystemMXBean) operatingSystemMXBean;

        metricRegistry.gauge(name("server.mem.free"), () -> sunOperatingSystemMXBean::getFreeMemorySize);

        return metricRegistry;
    }

    private void webServer(MetricRegistry metricRegistry) throws Exception {

        Configuration jettyConfig = configuration.getConfiguration("jetty");

        // Setup thread pool
        JettyConnectorThreadPool jettyConnectorThreadPool = new JettyConnectorThreadPool();
        jettyConnectorThreadPool.setName("jetty-http-server-");
        jettyConnectorThreadPool.setMinThreads(10);
        jettyConnectorThreadPool.setMaxThreads(150);

        // Create server
        server = new org.eclipse.jetty.server.Server(jettyConnectorThreadPool);

        // Setup connector
        final HttpConfiguration config = new HttpConfiguration();
        config.setOutputBufferSize(32768);
        config.setRequestHeaderSize(1024 * 16);

        // Added for X-Forwarded-For support, from ALB
        config.addCustomizer(new ForwardedRequestCustomizer());

        // Setup protocols
        HttpConnectionFactory http1 = new HttpConnectionFactory(config);
        final ServerConnector http = new ServerConnector(server, http1);
        http.setPort(jettyConfig.getInt("port", 8080));

        server.addConnector(http);

        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");

        ResourceConfig resourceConfig = new ResourceConfig();

        Binder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                try {
                    // Bind provided services
                    HttpClient httpClient = new HttpClient();
                    httpClient.start();
                    WebSocketClient webSocketClient = new WebSocketClient(httpClient);
                    webSocketClient.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));
                    webSocketClient.start();
                    RestClient restClient = new RestClient(webSocketClient);
                    bind(httpClient);
                    bind(webSocketClient);
                    bind(restClient);

                    bind(metricRegistry);
                    bind(configuration);
                    bind(ctx);
                    bind(Server.this);
                } catch (Exception e) {
                    throw new RuntimeIOException(e);
                }
            }
        };
        resourceConfig.register(binder);
        resourceConfig.packages("com.exoreaction.reactiveservices");

        ServletHolder serHol = new ServletHolder(new ServletContainer(resourceConfig));
        ctx.addServlet(serHol, "/*");
        serHol.setInitOrder(1);

        JettyWebSocketServletContainerInitializer.configure(ctx, null);

        InstrumentedHandler instrumentedHandler = new InstrumentedHandler(metricRegistry, "jetty");
        instrumentedHandler.setHandler(ctx);

        server.setHandler(instrumentedHandler);

        Slf4jRequestLogWriter requestLog = new Slf4jRequestLogWriter();
        requestLog.setLoggerName("jetty");
        server.setRequestLog(
                new CustomRequestLog(requestLog, "%{client}a - %u %t \"%r\" %s %O \"%{Referer}i\" \"%{User-Agent}i\""));

        // Start Jetty
        server.start();

        System.out.println(getServerDocument().toString());
    }

    public void close() throws IOException {
        try {
            server.stop();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
