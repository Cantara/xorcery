package com.exoreaction.reactiveservices.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.reactiveservices.jsonapi.Attributes;
import com.exoreaction.reactiveservices.jsonapi.JsonApiRels;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.ResourceObjects;
import com.exoreaction.reactiveservices.server.jetty.JettyConnectorThreadPool;
import com.exoreaction.reactiveservices.service.configuration.Configuration;
import com.exoreaction.reactiveservices.service.configuration.StandardConfiguration;
import com.exoreaction.reactiveservices.service.log4jappender.resources.LogWebSocketServlet;
import com.exoreaction.reactiveservices.service.registry.client.RegistryClient;
import com.exoreaction.reactiveservices.service.registry.resources.RegistryService;
import com.exoreaction.reactiveservices.service.registry.resources.websocket.RegistryWebSocketServlet;
import com.exoreaction.reactiveservices.service.soutlogger.SysoutLoggingService;
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
import java.net.URI;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

public class Server
    implements Closeable
{
    private Configuration configuration;
    private org.eclipse.jetty.server.Server server;
    private ResourceDocument serverDocument;
    private RegistryClient registryClient;

    public Server() throws Exception
    {
        this.configuration = configuration();

        webServer();
        register();
    }

    private Configuration configuration() throws Exception
    {
        Configuration.Builder builder = new Configuration.Builder();

        // Load defaults
        builder.addYaml( Configuration.class.getResourceAsStream( "/server.yaml" ) );

        // Load overrides
        StandardConfiguration standardConfiguration = new StandardConfiguration( builder.build() );

        File overridesYamlFile = new File( standardConfiguration.home(), "conf/server_overrides.yaml" );
        FileInputStream overridesYamlStream = new FileInputStream( overridesYamlFile );
        builder.addYaml( overridesYamlStream );

        // Load user
        File userYamlFile = new File( System.getProperty( "user.home" ), "reactive/server.yaml" );
        FileInputStream userYamlStream = new FileInputStream( userYamlFile );
        builder.addYaml( userYamlStream );

        return builder.build();
    }

    private void webServer() throws Exception
    {
        MetricRegistry metricRegistry = new MetricRegistry();

        // Setup Jvm mem metric
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOperatingSystemMXBean = (com.sun.management
            .OperatingSystemMXBean) operatingSystemMXBean;

        metricRegistry.gauge( name( "server.mem.free" ), () -> sunOperatingSystemMXBean::getFreeMemorySize );

        Configuration jettyConfig = configuration.getConfiguration( "jetty" );

        // Setup thread pool
        JettyConnectorThreadPool jettyConnectorThreadPool = new JettyConnectorThreadPool();
        jettyConnectorThreadPool.setName( "jetty-http-server-" );
        jettyConnectorThreadPool.setMinThreads( 10 );
        jettyConnectorThreadPool.setMaxThreads( 150 );

        // Create server
        server = new org.eclipse.jetty.server.Server( jettyConnectorThreadPool );

        // Setup connector
        final HttpConfiguration config = new HttpConfiguration();
        config.setOutputBufferSize( 32768 );
        config.setRequestHeaderSize( 1024 * 16 );

        // Added for X-Forwarded-For support, from ALB
        config.addCustomizer( new ForwardedRequestCustomizer() );

        // Setup protocols
        HttpConnectionFactory http1 = new HttpConnectionFactory( config );
        final ServerConnector http = new ServerConnector( server, http1 );
        http.setPort( jettyConfig.getInt( "port", 8080 ) );

        server.addConnector( http );

        ServletContextHandler ctx = new ServletContextHandler( ServletContextHandler.NO_SESSIONS );
        ctx.setContextPath( "/" );

        ResourceConfig resourceConfig = new ResourceConfig();

        Binder binder = new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                try
                {
                    // Bind provided services
                    bind( metricRegistry );

                    serverDocument = services( resourceConfig, this, ctx );
                    bind( serverDocument );
                }
                catch ( Exception e )
                {
                    throw new RuntimeIOException( e );
                }
            }
        };
        resourceConfig.register( binder );
        resourceConfig.packages( "com.exoreaction.reactiveservices" );

        ServletHolder serHol = new ServletHolder( new ServletContainer( resourceConfig ) );
        ctx.addServlet( serHol, "/*" );
        serHol.setInitOrder( 1 );

        JettyWebSocketServletContainerInitializer.configure( ctx, null );

        InstrumentedHandler instrumentedHandler = new InstrumentedHandler( metricRegistry, "jetty" );
        instrumentedHandler.setHandler( ctx );

        server.setHandler( instrumentedHandler );

        Slf4jRequestLogWriter requestLog = new Slf4jRequestLogWriter();
        requestLog.setLoggerName( "jetty" );
        server.setRequestLog(
            new CustomRequestLog( requestLog, "%{client}a - %u %t \"%r\" %s %O \"%{Referer}i\" \"%{User-Agent}i\"" ) );

        // Start Jetty
        server.start();
    }

    private ResourceDocument services( ResourceConfig resourceConfig, AbstractBinder binder,
                                       ServletContextHandler ctx ) throws Exception
    {
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        WebSocketClient webSocketClient = new WebSocketClient( httpClient );
        webSocketClient.start();

        registryClient = new RegistryClient( webSocketClient, URI.create( "http://localhost:8080/api/registry" ) );

        return new ResourceDocument.Builder()
            .links( new Links.Builder().link( JsonApiRels.self, "http://localhost:8080/" ).build() )
            .data( new ResourceObjects.Builder()
                .resource( new ResourceObject.Builder( "server" )
                    .attributes( new Attributes.Builder()
                        .attribute( "jetty.version", Jetty.VERSION )
                        .build() )
                    .build() )
                .resource( registry( resourceConfig, binder, ctx ) )
                .resource( log4jAppender( resourceConfig, binder, ctx ) )
                .resource( sysoutLogging( resourceConfig, binder, registryClient, webSocketClient ) )
                .resource( metrics( resourceConfig, binder, registryClient, webSocketClient ) )
                .build() )
            .build();
    }

    private ResourceObject metrics( ResourceConfig resourceConfig, AbstractBinder binder, RegistryClient registryClient,
                                    WebSocketClient webSocketClient )
    {
        resourceConfig.
        return null;
    }

    private ResourceObject registry( ResourceConfig resourceConfig, AbstractBinder binder,
                                     ServletContextHandler ctx )
    {
        var service = new RegistryService();
        binder.bind( service );
        ctx.addServlet( new ServletHolder( new RegistryWebSocketServlet( service ) ), "/ws/registryevents" );

        return new ResourceObject.Builder( "service", "registry" )
            .links( new Links.Builder()
                .link( "registry", URI.create( "http://localhost:8080/api/registry" ) )
                .link( "registryevents", URI.create( "ws://localhost:8080/ws/registryevents" ) )
                .build() )
            .build();
    }

    private ResourceObject log4jAppender( ResourceConfig resourceConfig, AbstractBinder binder,
                                          ServletContextHandler ctx )
    {
        ctx.addServlet( new ServletHolder( new LogWebSocketServlet() ), "/ws/logevents" );

        return new ResourceObject.Builder( "service", "log4jappender" )
            .links(
                new Links.Builder().link( "logevents", URI.create( "ws://localhost:8080/ws/logevents" ) ).build() )
            .build();
    }

    private ResourceObject sysoutLogging( ResourceConfig resourceConfig, AbstractBinder binder,
                                          RegistryClient registryClient, WebSocketClient webSocketClient )
    {
        binder.bind( SysoutLoggingService.class );

        return new ResourceObject.Builder( "service", "sysoutlogging" )
            .links( new Links.Builder().link( "logstore", URI.create( "http://localhost:8080/api/sysoutlogging" ) )
                                       .build() )
            .build();
    }

    public void register() throws Exception
    {
        registryClient.addServer( serverDocument );
    }

    public void close() throws IOException
    {
        try
        {
            server.stop();
        }
        catch ( Exception e )
        {
            throw new IOException( e );
        }
    }

}
