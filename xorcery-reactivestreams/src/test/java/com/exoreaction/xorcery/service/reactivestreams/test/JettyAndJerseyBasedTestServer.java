package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.ReactiveStreamsService;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.test.media.LongMessageBodyReader;
import com.exoreaction.xorcery.service.reactivestreams.test.media.LongMessageBodyWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.http.HttpClientConnectionFactory;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.time.Duration;
import java.util.List;

/**
 * A basic web-server based on jetty and jersey with the minimum needed to run websocket streams and servlets
 */
public class JettyAndJerseyBasedTestServer {

    private final Configuration configuration;
    private final Server server;
    private final ServletContextHandler ctx;
    private final WebSocketClient webSocketClient;
    private final ServletContainer servletContainer;
    private final ObjectMapper objectMapper;
    private ReactiveStreams reactiveStreams;

    public JettyAndJerseyBasedTestServer(Configuration configuration, List<Class<? extends MessageBodyWriter<?>>> writerClasses, List<Class<? extends MessageBodyReader<?>>> readerClasses) {
        this.configuration = configuration;

        this.server = createServer(configuration);

        ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");
        ResourceConfig resourceConfig = new ResourceConfig();
        for (Class<? extends MessageBodyWriter<?>> writerClass : writerClasses) {
            resourceConfig.register(writerClass, MessageBodyWriter.class);
        }
        for (Class<? extends MessageBodyReader<?>> readerClass : readerClasses) {
            resourceConfig.register(readerClass, MessageBodyReader.class);
        }
        servletContainer = new ServletContainer(resourceConfig);
        ServletHolder servletHolder = new ServletHolder(servletContainer);
        ctx.addServlet(servletHolder, "/*");
        servletHolder.setInitOrder(1);
        JettyWebSocketServletContainerInitializer.configure(ctx, null);

        HttpClient httpClient = createClient(configuration);
        webSocketClient = new WebSocketClient(httpClient);
        webSocketClient.setIdleTimeout(Duration.ofSeconds(httpClient.getIdleTimeout()));
        try {
            webSocketClient.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        server.addManaged(httpClient);

        server.setHandler(ctx);

        this.objectMapper = new ObjectMapper();
    }

    public void start() {
        try {
            server.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int httpPort = getHttpPort();

        InjectionManager injectionManager = servletContainer.getApplicationHandler().getInjectionManager();
        MessageBodyWorkers messageBodyWorkers = injectionManager.getInstance(MessageBodyWorkers.class);

        this.reactiveStreams = new ReactiveStreamsService(
                ctx,
                webSocketClient,
                configuration,
                server,
                messageBodyWorkers,
                objectMapper
        );

        System.out.printf("Server started and running http on port: %d%n", httpPort);
    }

    public void stop() {
        try {
            server.stop();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ReactiveStreams getReactiveStreams() {
        return reactiveStreams;
    }

    public int getHttpPort() {
        int port = -3;
        for (Connector connector : server.getConnectors()) {
            // the first connector should be the http connector
            ServerConnector serverConnector = (ServerConnector) connector;
            List<String> protocols = serverConnector.getProtocols();
            if (!protocols.contains("ssl") && (protocols.contains("http/1.1") || protocols.contains("h2c"))) {
                port = serverConnector.getLocalPort();
                break;
            }
        }
        return port;
    }

    private Server createServer(Configuration configuration) {
        Configuration jettyConfig = configuration.getConfiguration("server");

        int httpPort = jettyConfig.getInteger("http.port").orElse(0);
        int httpsPort = jettyConfig.getInteger("ssl.port").orElse(0);

        // Setup thread pool
        QueuedThreadPool jettyConnectorThreadPool = new QueuedThreadPool();
        jettyConnectorThreadPool.setName("jetty-http-server-");
        jettyConnectorThreadPool.setMinThreads(5);
        jettyConnectorThreadPool.setMaxThreads(10);

        // Create server
        Server server = new Server(jettyConnectorThreadPool);
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

        return server;
    }

    private HttpClient createClient(Configuration configuration) {
        // Client setup
        ClientConnector connector = new ClientConnector();
        connector.setIdleTimeout(Duration.ofSeconds(configuration.getLong("client.idle_timeout").orElse(-1L)));

        // HTTP 1.1
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = null;

        if (configuration.getBoolean("client.http2.enabled").orElse(false)) {
            // HTTP/2
            HTTP2Client http2Client = new HTTP2Client(connector);
            http2Client.setIdleTimeout(configuration.getLong("client.idle_timeout").orElse(-1L));

            http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
        }

        HttpClientTransportDynamic transport = null;
        // Figure out correct transport dynamics
        if (http2 != null) {
            transport = new HttpClientTransportDynamic(connector, http1, http2);
        } else {
            transport = new HttpClientTransportDynamic(connector, http1);
        }

        return new HttpClient(transport);
    }
}
