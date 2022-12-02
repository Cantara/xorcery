package com.exoreaction.xorcery.service.jetty.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.util.Resources;
import io.dropwizard.metrics.jetty11.InstrumentedHandler;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

@Service
public class JettyServerService
        implements Factory<ServletContextHandler> {
    private final Server server;
    private final ServletContextHandler servletContextHandler;
    private final Logger logger = LogManager.getLogger(getClass());

    @Inject
    public JettyServerService(Configuration configuration,
                              ServiceLocator serviceLocator,
                              Provider<MetricRegistry> metricRegistry) throws Exception {

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
//        server.setStopAtShutdown(false);

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

        // Clear-text protocols
        if (configuration.getBoolean("server.http2.enabled").orElse(false)) {
            // The ConnectionFactory for clear-text HTTP/2.
            HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

            // Create and configure the HTTP 1.1/2 connector
            final ServerConnector http = new ServerConnector(server, http11, h2c);
            http.setIdleTimeout(jettyConfig.getLong("idle_timeout").orElse(-1L));
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

            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            // The default protocol to use in case there is no negotiation.
            alpn.setDefaultProtocol(http11.getProtocol());

            sslContextFactory.setKeyStoreType(configuration.getString("server.ssl.keystore.type").orElse("PKCS12"));

            sslContextFactory.setKeyStorePath(configuration.getResourcePath("server.ssl.keystore.path")
                    .orElseGet(() -> Resources.getResource("META-INF/keystore.p12").orElseThrow().getPath()));
            sslContextFactory.setKeyStorePassword(configuration.getString("server.ssl.keystore.password").orElse("password"));

//            sslContextFactory.setTrustStoreType(configuration.getString("server.ssl.truststore.type").orElse("PKCS12"));
            sslContextFactory.setTrustStorePath(configuration.getResourcePath("server.ssl.truststore.path")
                    .orElseGet(() -> Resources.getResource("META-INF/truststore.jks").orElseThrow().getPath()));
            sslContextFactory.setTrustStorePassword(configuration.getString("server.ssl.truststore.password").orElse("password"));
            sslContextFactory.setHostnameVerifier((hostName, session) -> true);
            sslContextFactory.setTrustAll(configuration.getBoolean("server.ssl.trustall").orElse(false));
            sslContextFactory.setWantClientAuth(configuration.getBoolean("server.ssl.wantclientauth").orElse(false));

            SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

            // Create and configure the secure HTTP 1.1/2 connector, with ALPN negotiation
            ServerConnector https;
            if (configuration.getBoolean("server.http2.enabled").orElse(false)) {
                HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);
                https = new ServerConnector(server, tls, alpn, h2, http11);
            } else {
                https = new ServerConnector(server, tls, http11);
            }
            https.setIdleTimeout(jettyConfig.getLong("idle_timeout").orElse(-1L));
            https.setPort(httpsPort);
            server.addConnector(https);
        }

        Slf4jRequestLogWriter requestLog = new Slf4jRequestLogWriter();
        requestLog.setLoggerName("jetty");
        server.setRequestLog(
                new CustomRequestLog(requestLog, "%{client}a - %u %t \"%r\" %s %O \"%{Referer}i\" \"%{User-Agent}i\""));

        servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setAttribute("jersey.config.servlet.context.serviceLocator", serviceLocator);
        servletContextHandler.setContextPath("/");

        JettyWebSocketServletContainerInitializer.configure(servletContextHandler, null);

        Handler handler = servletContextHandler;
        if (configuration.getBoolean("metrics.enabled").orElse(false).equals(true)) {
            InstrumentedHandler instrumentedHandler = new InstrumentedHandler(metricRegistry.get(), "jetty");
            instrumentedHandler.setHandler(servletContextHandler);
            handler = instrumentedHandler;
        }

        server.setHandler(handler);
        server.start();

        ServiceLocatorUtilities.addOneConstant(serviceLocator, server);
    }

    @Override
    @Singleton
    @Named("server")
    public ServletContextHandler provide() {
        return servletContextHandler;
    }

    @Override
    public void dispose(ServletContextHandler instance) {
        try {
            logger.info("Stopping Jetty");
            server.stop();
        } catch (Throwable e) {
            logger.error(e);
        }
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
}
