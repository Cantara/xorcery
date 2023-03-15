package com.exoreaction.xorcery.service.jetty.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
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
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server")
@RunLevel(4)
public class JettyServerService
        implements Factory<ServletContextHandler>, PreDestroy {
    private final Server server;
    private final ServletContextHandler servletContextHandler;
    private final Logger logger = LogManager.getLogger(getClass());

    @Inject
    public JettyServerService(Configuration configuration,
                              ServiceLocator serviceLocator,
                              Provider<SslContextFactory.Server> sslContextFactoryProvider,
                              IterableProvider<SecurityHandler> securityHandlerProvider,
                              IterableProvider<MetricRegistry> metricRegistry) throws Exception {

        JettyServerConfiguration jettyConfig = new JettyServerConfiguration(configuration.getConfiguration("jetty.server"));
        JettyServerHttp2Configuration jettyHttp2Config = new JettyServerHttp2Configuration(configuration.getConfiguration("jetty.server.http2"));
        JettyServerSslConfiguration jettyServerSslConfiguration = new JettyServerSslConfiguration(configuration.getConfiguration("jetty.server.ssl"));

        int httpPort = jettyConfig.getHttpPort();
        int httpsPort = jettyServerSslConfiguration.getPort();

        // Setup thread pool
        JettyConnectorThreadPool jettyConnectorThreadPool = new JettyConnectorThreadPool();
        jettyConnectorThreadPool.setName("jetty-http-server-");
        jettyConnectorThreadPool.setMinThreads(jettyConfig.getMinThreads());
        jettyConnectorThreadPool.setMaxThreads(jettyConfig.getMaxThreads());

        // Create server
        server = new Server(jettyConnectorThreadPool);

        // Setup connector
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
        httpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

        // Added for X-Forwarded-For support, from ALB
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        // Setup protocols

        // Clear-text protocols
        HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);
        ServerConnector httpConnector;
        if (jettyHttp2Config.isEnabled()) {
            // The ConnectionFactory for clear-text HTTP/2.
            HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

            // Create and configure the HTTP 1.1/2 connector
            httpConnector = new ServerConnector(server, http11, h2c);
        } else {
            // Create and configure the HTTP 1.1 connector
            httpConnector = new ServerConnector(server, http11);
        }
        httpConnector.setIdleTimeout(jettyConfig.getIdleTimeout().toSeconds());
        httpConnector.setPort(httpPort);
        server.addConnector(httpConnector);

        if (jettyServerSslConfiguration.isEnabled()) {

            SslContextFactory.Server sslContextFactory = sslContextFactoryProvider.get();
            server.addBean(sslContextFactory);

            final HttpConfiguration sslHttpConfig = new HttpConfiguration();
            sslHttpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
            sslHttpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

            SecureRequestCustomizer customizer = new SecureRequestCustomizer();
            customizer.setSniRequired(jettyServerSslConfiguration.isSniRequired());
            customizer.setSniHostCheck(jettyServerSslConfiguration.isSniHostCheck());
            sslHttpConfig.addCustomizer(customizer);

            // Added for X-Forwarded-For support, from ALB
            sslHttpConfig.addCustomizer(new ForwardedRequestCustomizer());

            HttpConnectionFactory sslHttp11 = new HttpConnectionFactory(sslHttpConfig);

            // The ALPN ConnectionFactory.
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            // The default protocol to use in case there is no negotiation.
            alpn.setDefaultProtocol(sslHttp11.getProtocol());

            SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

            // Create and configure the secure HTTP 1.1/2 connector, with ALPN negotiation
            ServerConnector httpsConnector;
            if (jettyHttp2Config.isEnabled()) {
                HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(sslHttpConfig);
                httpsConnector = new ServerConnector(server, tls, alpn, h2, sslHttp11);
            } else {
                httpsConnector = new ServerConnector(server, tls, alpn, sslHttp11);
            }
            httpsConnector.setIdleTimeout(jettyConfig.getIdleTimeout().toSeconds());
            httpsConnector.setPort(httpsPort);
            server.addConnector(httpsConnector);
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
        if (securityHandlerProvider.getHandle() != null)
        {
            SecurityHandler securityHandler = securityHandlerProvider.get();
            securityHandler.setHandler(handler);
            handler = securityHandler;
        }

        if (metricRegistry.getHandle() != null) {
            InstrumentedHandler instrumentedHandler = new InstrumentedHandler(metricRegistry.get(), "jetty");
            instrumentedHandler.setHandler(handler);
            handler = instrumentedHandler;
        }

        server.setHandler(handler);
        server.start();

        ServiceLocatorUtilities.addOneConstant(serviceLocator, server);


        logger.info("Started Jetty server");
    }

    @Override
    public void preDestroy() {
        logger.info("Stopping Jetty server");
        try {
            server.stop();
        } catch (Throwable e) {
            logger.error(e);
        }
    }

    @Override
    @Singleton
    @Named("jetty.server")
    public ServletContextHandler provide() {
        return servletContextHandler;
    }

    @Override
    public void dispose(ServletContextHandler instance) {
    }
}
