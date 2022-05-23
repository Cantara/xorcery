package com.exoreaction.reactiveservices.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.configuration.StandardConfiguration;
import com.exoreaction.reactiveservices.jetty.server.JettyConnectorThreadPool;
import com.exoreaction.reactiveservices.jsonapi.model.*;
import com.exoreaction.reactiveservices.rest.RestClient;
import com.exoreaction.reactiveservices.server.resources.ServerApplication;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import io.dropwizard.metrics.jetty11.InstrumentedHandler;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

public class Server
        implements Closeable {
    private final String serverId;
    private Configuration configuration;
    private URI baseUri;

    private Marker serverLogMarker;
    private org.eclipse.jetty.server.Server server;
    private List<ResourceObject> services = new CopyOnWriteArrayList<>();

    public Server(File configurationFile, String id) throws Exception {
        this.configuration = configuration(configurationFile);

        serverId = configuration.getString("id").orElseGet(()-> Optional.ofNullable(id).orElse(UUID.randomUUID().toString()));

        serverLogMarker = MarkerManager.getMarker("server:"+ serverId);

        services.add(new ResourceObject.Builder("server", serverId)
                        .attributes(new Attributes.Builder()
                                .attribute("jetty.version", Jetty.VERSION)
                                .build()).build());
        MetricRegistry metricRegistry = metrics();

        webServer(metricRegistry);
    }

    public String getServerId() {
        return serverId;
    }

    public void addService(ResourceObject serviceResource) {
        services.add(serviceResource);
    }
    public Marker getServerLogMarker() {
        return serverLogMarker;
    }

    public UriBuilder getBaseUriBuilder()
    {
        return UriBuilder.fromUri(baseUri);
    }

    public ResourceDocument getServerDocument()
    {
        ResourceDocument serverDocument = new ResourceDocument.Builder()
                .links(new Links.Builder().link(JsonApiRels.self, baseUri).build())
                .data(services.stream().collect(ResourceObjects.toResourceObjects()))
                .build();
        return serverDocument;
    }

    private Configuration configuration(File configFile) throws Exception {
        Configuration.Builder builder = new Configuration.Builder();

        // Load defaults
        if (configFile != null)
        {
            builder = builder.addYaml(new FileInputStream(configFile));
        } else {
            builder = builder.addYaml(Configuration.class.getResourceAsStream("/server.yaml"));
        }

        // Load overrides
        Configuration partialConfig = builder.build();
        StandardConfiguration standardConfiguration = new StandardConfiguration(partialConfig);
        builder = partialConfig.asBuilder();
        File overridesYamlFile = new File(standardConfiguration.home(), "conf/server_overrides.yaml");
        if (overridesYamlFile.exists()) {
            FileInputStream overridesYamlStream = new FileInputStream(overridesYamlFile);
            builder = builder.addYaml(overridesYamlStream);
        }

        // Load user
        File userYamlFile = new File(System.getProperty("user.home"), "reactive/server.yaml");
        if (userYamlFile.exists()) {
            FileInputStream userYamlStream = new FileInputStream(userYamlFile);
            builder = builder.addYaml(userYamlStream);
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

        Configuration jettyConfig = configuration.getConfiguration("server");

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
        http.setPort(jettyConfig.getInteger("port").orElse(8889));

        baseUri = URI.create("http://127.0.0.1:"+http.getPort()+"/");

        server.addConnector(http);

        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");

        ServerApplication app = new ServerApplication();

        Binder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                try {
                    // Bind provided services
                    HttpClient httpClient = new HttpClient();
                    httpClient.start();
                    server.addManaged(httpClient);
                    WebSocketClient webSocketClient = new WebSocketClient(httpClient);
                    webSocketClient.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));
                    webSocketClient.start();
                    server.addManaged(webSocketClient);
                    RestClient restClient = new RestClient(webSocketClient);
                    bind(httpClient);
                    bind(webSocketClient);
                    bind(restClient);

                    // Create default ObjectMapper
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                    objectMapper.registerModule(new JSONPModule());
                    bind(objectMapper);

                    bind(metricRegistry);
                    bind(configuration);
                    bind(ctx);
                    bind(Server.this);
                } catch (Exception e) {
                    throw new RuntimeIOException(e);
                }
            }
        };
        app.register(binder);
        app.packages("com.exoreaction.reactiveservices.service");

        ServletHolder serHol = new ServletHolder(new ServletContainer(app));
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
    }

    public void close() throws IOException {
        try {
            server.stop();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
