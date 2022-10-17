package com.exoreaction.xorcery.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jetty.server.JettyConnectorThreadPool;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.resources.ServerApplication;
import com.exoreaction.xorcery.util.UUIDs;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.metrics.jetty11.InstrumentedHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.inject.hk2.ImmediateHk2InjectionManager;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.eclipse.jetty.util.ssl.SslContextFactory.Client.SniProvider.NON_DOMAIN_SNI_PROVIDER;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */
public class Xorcery
        implements Closeable {

    private static final Logger logger = LogManager.getLogger(Xorcery.class);

    private Server server;
    private ServletContainer servletContainer;

    private ServiceLocator serviceLocator;

    public Xorcery(File configurationFile, String id) throws Exception {
        Configuration configuration = createConfiguration(configurationFile);

        // Ensure this server has an id
        configuration.getString("id").orElseGet(() ->
        {
            String newId = Optional.ofNullable(id).orElse(UUIDs.newId());
            configuration.json().set("id", configuration.json().textNode(newId));
            return newId;
        });

        initialize(configuration);
    }

    public Xorcery(Configuration configuration) throws Exception {
        ObjectWriter objectWriter = new ObjectMapper(new YAMLFactory()).writer().withDefaultPrettyPrinter();
        logger.info("Resolved configuration:\n" + objectWriter.writeValueAsString(configuration.json()));
        initialize(configuration);
    }

    protected void initialize(Configuration configuration) throws Exception {

        serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.addOneConstant(serviceLocator, this);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, configuration);

/*
        DynamicConfigurationService dcs = serviceLocator.getService(DynamicConfigurationService.class);
        DynamicConfiguration dc = dcs.createDynamicConfiguration();
*/

        List<?> services = serviceLocator.getAllServices(d -> true);
        services.forEach(System.out::println);
        MetricRegistry metricRegistry = createMetrics(configuration);

        HttpClient client = createClient(configuration, metricRegistry);
//        client.start();

        Server server = createServer(configuration, metricRegistry);
        Handler handler = createHandler(configuration, metricRegistry, client, server);

        server.setHandler(handler);

        ServiceLocatorUtilities.addOneConstant(serviceLocator, server, "Server", Server.class);

        server.start();

        // Expose application InjectionManager as the ServiceLocator
        serviceLocator = ((ImmediateHk2InjectionManager) servletContainer.getApplicationHandler().getInjectionManager()).getServiceLocator();
    }

    protected Configuration createConfiguration(File configFile) throws Exception {
        StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
        Configuration.Builder builder = new Configuration.Builder()
                .with(standardConfigurationBuilder::addDefaults, standardConfigurationBuilder.addFile(configFile));

        // Log final configuration
        ObjectWriter objectWriter = new ObjectMapper(new YAMLFactory()).writer().withDefaultPrettyPrinter();
        logger.debug("Configuration:\n" + objectWriter.writeValueAsString(builder.builder()));

        Configuration configuration = builder.build();
        logger.info("Resolved configuration:\n" + objectWriter.writeValueAsString(configuration.json()));

        return configuration;
    }

    protected MetricRegistry createMetrics(Configuration configuration) {
        MetricRegistry metricRegistry = new MetricRegistry();

        // Setup Jvm mem metric
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOperatingSystemMXBean = (com.sun.management
                .OperatingSystemMXBean) operatingSystemMXBean;

        metricRegistry.gauge(MetricRegistry.name("server.mem.free"), () -> sunOperatingSystemMXBean::getFreeMemorySize);

        return metricRegistry;
    }

    protected HttpClient createClient(Configuration configuration, MetricRegistry metricRegistry) throws Exception {
        // Client setup
        ClientConnector connector = new ClientConnector();
        connector.setIdleTimeout(Duration.ofSeconds(configuration.getLong("client.idle_timeout").orElse(-1L)));

        // HTTP 1.1
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = null;
        ClientConnectionFactoryOverHTTP3.HTTP3 http3 = null;

        if (configuration.getBoolean("client.http2.enabled").orElse(false)) {
            // HTTP/2
            HTTP2Client http2Client = new HTTP2Client(connector);
            http2Client.setIdleTimeout(configuration.getLong("client.idle_timeout").orElse(-1L));

            http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
        }

        HttpClientTransportDynamic transport = null;
        if (configuration.getBoolean("client.ssl.enabled").orElse(false)) {

            SslContextFactory.Client sslClientContextFactory = new SslContextFactory.Client() {
                @Override
                protected KeyStore loadTrustStore(Resource resource) throws Exception {
                    KeyStore keyStore = super.loadTrustStore(resource);
                    addDefaultRootCaCertificates(keyStore);
                    return keyStore;
                }
            };
            sslClientContextFactory.setKeyStoreType(configuration.getString("client.ssl.keystore.type").orElse("PKCS12"));
            sslClientContextFactory.setKeyStorePath(configuration.getString("client.ssl.keystore.path")
                    .orElseGet(() -> getClass().getResource("/keystore.p12").toExternalForm()));
            sslClientContextFactory.setKeyStorePassword(configuration.getString("client.ssl.keystore.password").orElse("password"));

//                        sslClientContextFactory.setTrustStoreType(configuration.getString("client.ssl.truststore.type").orElse("PKCS12"));
            sslClientContextFactory.setTrustStorePath(configuration.getString("client.ssl.truststore.path")
                    .orElseGet(() -> getClass().getResource("/keystore.p12").toExternalForm()));
            sslClientContextFactory.setTrustStorePassword(configuration.getString("client.ssl.truststore.password").orElse("password"));

            sslClientContextFactory.setEndpointIdentificationAlgorithm("HTTPS");
            sslClientContextFactory.setHostnameVerifier((hostName, session) -> true);
            sslClientContextFactory.setTrustAll(configuration.getBoolean("client.ssl.trustall").orElse(false));
            sslClientContextFactory.setSNIProvider(NON_DOMAIN_SNI_PROVIDER);


            connector.setSslContextFactory(sslClientContextFactory);

            // HTTP/3
            if (configuration.getBoolean("client.http3.enabled").orElse(false)) {
                HTTP3Client h3Client = new HTTP3Client();
                h3Client.getClientConnector().setIdleTimeout(Duration.ofSeconds(configuration.getLong("client.idle_timeout").orElse(-1L)));
                h3Client.getQuicConfiguration().setSessionRecvWindow(64 * 1024 * 1024);
                http3 = new ClientConnectionFactoryOverHTTP3.HTTP3(h3Client);
                h3Client.getClientConnector().setSslContextFactory(sslClientContextFactory);
            }
        }

        // Figure out correct transport dynamics
        if (http3 != null) {
            if (http2 != null) {
                transport = new HttpClientTransportDynamic(connector, http1, http3, http2);
            } else {
                transport = new HttpClientTransportDynamic(connector, http1, http3);
            }
        } else if (http2 != null) {
            transport = new HttpClientTransportDynamic(connector, http1, http2);
        } else {
            transport = new HttpClientTransportDynamic(connector, http1);
        }

        return new HttpClient(transport);
    }

    protected Server createServer(Configuration configuration, MetricRegistry metricRegistry) throws Exception {

        Configuration jettyConfig = configuration.getConfiguration("server");

        int httpPort = jettyConfig.getInteger("http.port").orElse(8889);
        int httpsPort = jettyConfig.getInteger("ssl.port").orElse(8443);

        // Setup thread pool
        JettyConnectorThreadPool jettyConnectorThreadPool = new JettyConnectorThreadPool();
        jettyConnectorThreadPool.setName("jetty-http-server-");
        jettyConnectorThreadPool.setMinThreads(10);
        jettyConnectorThreadPool.setMaxThreads(150);

        // Create server
        server = new Server(jettyConnectorThreadPool);
        server.setStopAtShutdown(true);

        // Setup connector
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(1024 * 16);

        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniRequired(configuration.getBoolean("server.ssl.snirequired").orElse(true));
        customizer.setSniHostCheck(configuration.getBoolean("server.ssl.snihostcheck").orElse(true));
        httpConfig.addCustomizer(customizer);

        // Added for X-Forwarded-For support, from ALB
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        // Setup protocols
        HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

        if (configuration.getBoolean("server.http2.enabled").orElse(false)) {
            // The ConnectionFactory for clear-text HTTP/2.
            HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

            // Create and configure the HTTP 1.1/2 connector
            final ServerConnector http = new ServerConnector(server, http11, h2c);
            http.setPort(httpPort);
            server.addConnector(http);
        } else {
            // Create and configure the HTTP 1.1 connector
            final ServerConnector http = new ServerConnector(server, http11);
            http.setPort(httpPort);
            server.addConnector(http);
        }

        // Configure the SslContextFactory with the keyStore information.
        SslContextFactory.Server sslContextFactory = configuration.getBoolean("server.ssl.enabled").orElse(false) ?
                new SslContextFactory.Server() {
                    @Override
                    protected KeyStore loadTrustStore(Resource resource) throws Exception {
                        KeyStore keyStore = super.loadTrustStore(resource);
                        addDefaultRootCaCertificates(keyStore);
                        return keyStore;
                    }
                } : null;

        if (configuration.getBoolean("server.ssl.enabled").orElse(false)) {
            // The ALPN ConnectionFactory.
/*
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            // The default protocol to use in case there is no negotiation.
            alpn.setDefaultProtocol(http11.getProtocol());
*/

            sslContextFactory.setKeyStoreType(configuration.getString("server.ssl.keystore.type").orElse("PKCS12"));
            sslContextFactory.setKeyStorePath(configuration.getString("server.ssl.keystore.path")
                    .orElseGet(() -> getClass().getResource("/keystore.p12").toExternalForm()));
            sslContextFactory.setKeyStorePassword(configuration.getString("server.ssl.keystore.password").orElse("password"));

//            sslContextFactory.setTrustStoreType(configuration.getString("server.ssl.truststore.type").orElse("PKCS12"));
            sslContextFactory.setTrustStorePath(configuration.getString("server.ssl.truststore.path")
                    .orElseGet(() -> getClass().getResource("/keystore.p12").toExternalForm()));
            sslContextFactory.setTrustStorePassword(configuration.getString("server.ssl.truststore.password").orElse("password"));
            sslContextFactory.setHostnameVerifier((hostName, session) -> true);
            sslContextFactory.setTrustAll(configuration.getBoolean("server.ssl.trustall").orElse(false));
            sslContextFactory.setWantClientAuth(configuration.getBoolean("server.ssl.wantclientauth").orElse(false));

//            SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
            SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, http11.getProtocol());

            // Create and configure the secure HTTP 1.1/2 connector
            ServerConnector https;
            if (configuration.getBoolean("server.http2.enabled").orElse(false)) {
                HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);
                https = new ServerConnector(server, tls, h2, http11);
            } else {
                https = new ServerConnector(server, tls, http11);
            }
            https.setIdleTimeout(jettyConfig.getLong("idle_timeout").orElse(-1L));
            https.setPort(httpsPort);
            server.addConnector(https);

            // Create and configure the HTTP/3 connector
            if (configuration.getBoolean("server.http3.enabled").orElse(false)) {
                HTTP3ServerConnector connector = new HTTP3ServerConnector(server, sslContextFactory, new HTTP3ServerConnectionFactory(httpConfig));
                connector.setIdleTimeout(jettyConfig.getLong("idle_timeout").orElse(-1L));
                connector.setPort(configuration.getInteger("server.http3.port").orElse(httpsPort));
                server.addConnector(connector);
            }
        }

        Slf4jRequestLogWriter requestLog = new Slf4jRequestLogWriter();
        requestLog.setLoggerName("jetty");
        server.setRequestLog(
                new CustomRequestLog(requestLog, "%{client}a - %u %t \"%r\" %s %O \"%{Referer}i\" \"%{User-Agent}i\""));

        return server;
    }

    protected Handler createHandler(Configuration configuration, MetricRegistry metricRegistry, HttpClient client, Server server)
            throws IOException {
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setAttribute("jersey.config.servlet.context.serviceLocator", serviceLocator);
        ctx.setContextPath("/");

        ServerApplication app = new ServerApplication();

        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                if (client.getSslContextFactory() != null)
                    bind(client.getSslContextFactory());
                Xorcery.this.server.addManaged(client);
                bind(new JettyHttpClientSupplier(client)).to(JettyHttpClientContract.class);

                // Create default ObjectMapper
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                bind(objectMapper);

                bind(metricRegistry);
                bind(ctx);

                StandardConfiguration standardConfiguration = () -> configuration;
                bind(new ResourceObject.Builder("server", standardConfiguration.getId())
                        .attributes(new Attributes.Builder()
                                .attribute("jetty.version", Jetty.VERSION)
                                .build()).build()).named("server");

//                    if (server.getConnectors()[0].getDefaultConnectionFactory().sslContextFactory != null)
//                        bind(sslContextFactory);
            }
        };

        app.register(binder);

        configuration.getList("jaxrs.register").ifPresent(jsonNodes ->
        {
            for (JsonNode jsonNode : jsonNodes) {
                try {
                    app.register(getClass().getClassLoader().loadClass(jsonNode.asText()));
                } catch (ClassNotFoundException e) {
                    logger.error("Could not load JAX-RS provider " + jsonNode.asText(), e);
                    throw new RuntimeException(e);
                }
            }
        });

        configuration.getList("jaxrs.packages").ifPresent(jsonNodes ->
        {
            for (JsonNode jsonNode : jsonNodes) {
                app.packages(jsonNode.asText());
            }
        });

        servletContainer = new ServletContainer(app);

        ServletHolder servletHolder = new ServletHolder(servletContainer);
        ctx.addServlet(servletHolder, "/*");
        servletHolder.setInitOrder(1);

        JettyWebSocketServletContainerInitializer.configure(ctx, null);

        InstrumentedHandler instrumentedHandler = new InstrumentedHandler(metricRegistry, "jetty");
        instrumentedHandler.setHandler(ctx);
        return instrumentedHandler;
    }

    protected void addDefaultRootCaCertificates(KeyStore trustStore) throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        // Loads default Root CA certificates (generally, from JAVA_HOME/lib/cacerts)
        trustManagerFactory.init((KeyStore) null);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                for (X509Certificate acceptedIssuer : ((X509TrustManager) trustManager).getAcceptedIssuers()) {
                    trustStore.setCertificateEntry(acceptedIssuer.getSubjectDN().getName(), acceptedIssuer);
                }
            }
        }
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public void close() throws IOException {
        try {
            server.stop();
        } catch (Exception e) {
            throw new IOException(e);
        }

        serviceLocator.getParent().shutdown();
    }
}
