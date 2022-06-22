package com.exoreaction.xorcery.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.StandardConfiguration;
import com.exoreaction.xorcery.cqrs.UUIDs;
import com.exoreaction.xorcery.jetty.server.JettyConnectorThreadPool;
import com.exoreaction.xorcery.jsonapi.model.*;
import com.exoreaction.xorcery.server.resources.ServerApplication;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import io.dropwizard.metrics.jetty11.InstrumentedHandler;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.http.HttpClientConnectionFactory;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.http.ClientConnectionFactoryOverHTTP3;
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory;
import org.eclipse.jetty.http3.server.HTTP3ServerConnector;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.codahale.metrics.MetricRegistry.name;
import static org.eclipse.jetty.util.ssl.SslContextFactory.Client.SniProvider.NON_DOMAIN_SNI_PROVIDER;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

public class Server
        implements Closeable {

    private static final Logger logger = LogManager.getLogger(Server.class);

    private final String serverId;
    private Configuration configuration;
    private URI baseUri;

    private Marker serverLogMarker;
    private org.eclipse.jetty.server.Server server;
    private List<ResourceObject> services = new CopyOnWriteArrayList<>();

    public Server(File configurationFile, String id) throws Exception {
        this.configuration = configuration(configurationFile);

        serverId = configuration.getString("id").orElseGet(() -> Optional.ofNullable(id).orElse(UUIDs.newId()));

        serverLogMarker = MarkerManager.getMarker("server:" + serverId);

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

    public URI getBaseUri() {
        return baseUri;
    }

    public UriBuilder getBaseUriBuilder() {
        return UriBuilder.fromUri(baseUri);
    }

    public ResourceDocument getServerDocument() {
        ResourceDocument serverDocument = new ResourceDocument.Builder()
                .links(new Links.Builder().link(JsonApiRels.self, baseUri).build())
                .data(services.stream().collect(ResourceObjects.toResourceObjects()))
                .build();
        return serverDocument;
    }

    public org.eclipse.jetty.server.Server getServer()
    {
        return server;
    }

    private Configuration configuration(File configFile) throws Exception {
        Configuration.Builder builder = new Configuration.Builder();

        // Load system properties and environment variables
        builder.addSystemProperties("SYSTEM");
        builder.addEnvironmentVariables("ENV");

        // Load defaults
        if (configFile != null) {
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

        // Log final configuration
        StringWriter out = new StringWriter();
        new ObjectMapper(new YAMLFactory()).writer().withDefaultPrettyPrinter().writeValue(out, builder.builder());

        logger.debug("Configuration:\n"+out);

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

/*
        // Ensure Conscrypt is used
        ConscryptHostnameVerifier hostnameVerifier = new ConscryptHostnameVerifier() {
            @Override
            public boolean verify(X509Certificate[] certs, String hostname, SSLSession session) {
                return true;
            }
        };
        Conscrypt.setDefaultHostnameVerifier(hostnameVerifier);
        java.security.Security.insertProviderAt(new org.conscrypt.OpenSSLProvider(), 1);
*/

        // Setup thread pool
        JettyConnectorThreadPool jettyConnectorThreadPool = new JettyConnectorThreadPool();
        jettyConnectorThreadPool.setName("jetty-http-server-");
        jettyConnectorThreadPool.setMinThreads(10);
        jettyConnectorThreadPool.setMaxThreads(150);

        // Create server
        server = new org.eclipse.jetty.server.Server(jettyConnectorThreadPool);
        server.setStopAtShutdown(true);

        // Setup connector
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(1024 * 16);
        httpConfig.addCustomizer(new SecureRequestCustomizer());

        // Added for X-Forwarded-For support, from ALB
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        // Setup protocols
        HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

        // The ConnectionFactory for clear-text HTTP/2.
        HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

        // Create and configure the HTTP 1.1/2 connector
        final ServerConnector http = new ServerConnector(server, http11, h2c);
        http.setPort(jettyConfig.getInteger("port").orElse(8889));
        server.addConnector(http);

        // The ALPN ConnectionFactory.
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        // The default protocol to use in case there is no negotiation.
        alpn.setDefaultProtocol(http11.getProtocol());

        // Configure the SslContextFactory with the keyStore information.
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreType("PKCS12");
        sslContextFactory.setKeyStorePath(new File(".").toURI().relativize(getClass().getResource("/keystore.p12").toURI()).getPath());
        sslContextFactory.setKeyStorePassword("password");
        sslContextFactory.setTrustStorePath(new File(".").toURI().relativize(getClass().getResource("/truststore.jks").toURI()).getPath());
        sslContextFactory.setTrustStorePassword("password");
        sslContextFactory.setHostnameVerifier((hostName, session) -> true);
        sslContextFactory.setTrustAll(true);
        sslContextFactory.setWantClientAuth(true);

        SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

        // Create and configure the secure HTTP 1.1/2 connector
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);
        final ServerConnector https = new ServerConnector(server, tls, alpn, h2, http11);
        https.setIdleTimeout(jettyConfig.getLong("idle_timeout").orElse(-1L));
//        final ServerConnector https = new ServerConnector(server, tls, alpn, http11);
        https.setPort(jettyConfig.getInteger("secure_port").orElse(8443));
        server.addConnector(https);

        // Create and configure the HTTP/3 connector
        HTTP3ServerConnector connector = new HTTP3ServerConnector(server, sslContextFactory, new HTTP3ServerConnectionFactory(httpConfig));
        connector.setIdleTimeout(jettyConfig.getLong("idle_timeout").orElse(-1L));
        connector.setPort(jettyConfig.getInteger("secure_port").orElse(8443));
        server.addConnector(connector);

        baseUri = URI.create("https://"+configuration.getString("host").orElse("localhost")+":" + https.getPort() + "/");


        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");

        ServerApplication app = new ServerApplication();

        Binder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                try {

                    // Client setup
                    SslContextFactory.Client sslClientContextFactory = new SslContextFactory.Client();
                    sslClientContextFactory.setKeyStoreType("PKCS12");
                    sslClientContextFactory.setKeyStorePath(new File(".").toURI().relativize(getClass().getResource("/keystore.p12").toURI()).getPath());
                    sslClientContextFactory.setKeyStorePassword("password");
                    sslClientContextFactory.setTrustStorePath(new File(".").toURI().relativize(getClass().getResource("/truststore.jks").toURI()).getPath());
                    sslClientContextFactory.setTrustStorePassword("password");
                    sslClientContextFactory.setEndpointIdentificationAlgorithm("HTTPS");
                    sslClientContextFactory.setHostnameVerifier((hostName, session) -> true);
                    sslClientContextFactory.setTrustAll(true);
                    sslClientContextFactory.setSNIProvider(NON_DOMAIN_SNI_PROVIDER);

                    ClientConnector connector = new ClientConnector();
                    connector.setIdleTimeout(Duration.ofMillis(jettyConfig.getLong("idle_timeout").orElse(-1L)));
                    connector.setSslContextFactory(sslClientContextFactory);

                    // HTTP 1.1
                    ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

                    // HTTP/2
                    HTTP2Client http2Client = new HTTP2Client(connector);
                    ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);

                    // HTTP/3
                    HTTP3Client h3Client = new HTTP3Client();
                    h3Client.getClientConnector().setSslContextFactory(sslClientContextFactory);
                    h3Client.getClientConnector().setIdleTimeout(Duration.ofMillis(jettyConfig.getLong("idle_timeout").orElse(-1L)));
                    h3Client.getQuicConfiguration().setSessionRecvWindow(64 * 1024 * 1024);
                    ClientConnectionFactoryOverHTTP3.HTTP3 http3 = new ClientConnectionFactoryOverHTTP3.HTTP3(h3Client);

//                    HttpClientTransportDynamic transport = new HttpClientTransportDynamic(connector, http1);
                    HttpClientTransportDynamic transport = new HttpClientTransportDynamic(connector, http1, http2, http3);
//                    HttpClientTransportOverHTTP3 transport = new HttpClientTransportOverHTTP3(h3Client);
                    HttpClient client = new HttpClient(transport);
                    client.start();
                    server.addManaged(client);
                    bind(new JettyHttpClientSupplier(client)).to(JettyHttpClientContract.class);

                    // Create default ObjectMapper
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    objectMapper.registerModule(new JSONPModule());
                    bind(objectMapper);

                    bind(metricRegistry);
                    bind(configuration);
                    bind(ctx);
                    bind(Server.this);

                    bind(sslContextFactory);
                    bind(sslClientContextFactory);

                } catch (Exception e) {
                    throw new RuntimeIOException(e);
                }
            }
        };
        app.register(binder);

        configuration.getList("jaxrs.register").ifPresent(jsonNodes ->
        {
            for (JsonNode jsonNode : jsonNodes) {
                try {
                    app.register(getClass().getClassLoader().loadClass(jsonNode.asText()));
                } catch (ClassNotFoundException e) {
                    logger.error("Could not load JAX-RS provider "+jsonNode.asText(), e);
                }
            }
        });

        configuration.getList("jaxrs.packages").ifPresent(jsonNodes ->
        {
            for (JsonNode jsonNode : jsonNodes) {
                app.packages(jsonNode.asText());
            }
        });

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
