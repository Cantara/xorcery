package com.exoreaction.reactiveservices.service.soutlogger;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.disruptor.MetadataDeserializerEventHandler;
import com.exoreaction.reactiveservices.disruptor.WebSocketFlowControlEventHandler;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.service.registry.client.RegistryClient;
import com.exoreaction.reactiveservices.service.registry.client.RegistryListener;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ForkJoinPool;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Provider
public class SysoutLoggingService
    implements Closeable, ContainerLifecycleListener
{
    @Override
    public void onStartup( Container container )
    {
        System.out.println( "Startup" );
        start();
    }

    @Override
    public void onReload( Container container )
    {

    }

    @Override
    public void onShutdown( Container container )
    {
        System.out.println( "Shutdown" );
    }

    private final RegistryClient registryClient;
    private final WebSocketClient webSocketClient;

    @Inject
    public SysoutLoggingService( RegistryClient registryClient, WebSocketClient webSocketClient )
    {
        this.registryClient = registryClient;
        this.webSocketClient = webSocketClient;
    }

    public void start()
    {
        ForkJoinPool.commonPool().execute( () ->
        {
            registryClient.addRegistryListener( new LoggingRegistryListener() );
        } );
    }

    public void connect( Link logSource )
    {
        try
        {
            Disruptor<EventHolder<LogEvent>> disruptor =
                new Disruptor<>( EventHolder::new, 4096, new NamedThreadFactory( "SysoutDisruptorIn-" ),
                    ProducerType.SINGLE,
                    new YieldingWaitStrategy() );

            Session session =
                webSocketClient.connect( new LoggingClientEndpoint( disruptor ), URI.create( logSource.getHref() ) )
                               .get();

            disruptor.handleEventsWith(new MetadataDeserializerEventHandler(),
                new Log4jDeserializeEventHandler( new JsonLogEventParser() ))
                .then( new SysoutLogEventHandler(), new WebSocketFlowControlEventHandler( 4096, session ) );
            disruptor.start();

            System.out.println( "Receiving log messages from " + logSource.getHref() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException
    {
        // TODO Close active sessions
    }

    private class LoggingRegistryListener implements RegistryListener
    {
        @Override
        public void added( ResourceObject service )
        {
            service.getLinks().getRel( "logevents" ).ifPresent( SysoutLoggingService.this::connect );
        }

        @Override
        public void updated( ResourceObject service )
        {

        }

        @Override
        public void removed( ResourceObject service )
        {

        }
    }

    private class LoggingClientEndpoint
        implements WebSocketListener
    {
        private final Disruptor<EventHolder<LogEvent>> disruptor;

        ByteBuffer headers;

        private LoggingClientEndpoint(
            Disruptor<EventHolder<LogEvent>> disruptor )
        {
            this.disruptor = disruptor;
        }

        @Override
        public void onWebSocketText( String message )
        {
            System.out.println( "Text:" + message );
        }

        @Override
        public void onWebSocketBinary( byte[] payload, int offset, int len )
        {
            if ( headers == null )
            {
                headers = ByteBuffer.wrap( payload, offset, len );
            }
            else
            {
                ByteBuffer body = ByteBuffer.wrap( payload, offset, len );
                disruptor.publishEvent( ( holder, seq, h, b ) ->
                {
                    holder.headers = h;
                    holder.body = b;
                }, headers, body );
                headers = null;
            }
        }

        @Override
        public void onWebSocketClose( int statusCode, String reason )
        {
            System.out.printf( "Close:%d %s%n", statusCode, reason );
            disruptor.shutdown();
        }

        @Override
        public void onWebSocketConnect( Session session )
        {
            System.out.printf( "Connect:%s%n", session.getRemote().getRemoteAddress().toString() );
        }

        @Override
        public void onWebSocketError( Throwable cause )
        {
            StringWriter sw = new StringWriter();
            cause.printStackTrace( new PrintWriter( sw ) );

            System.out.printf( "Error:%s%n", sw.toString() );
        }
    }
}
