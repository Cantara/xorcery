package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.Marker;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.lang.reflect.Type;
import java.time.Duration;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class SubscriberWebSocketServlet<T>
        extends JettyWebSocketServlet {
    private String path;
    private ReactiveEventStreams.Subscriber<T> subscriber;
    private MessageBodyWriter<Object> messageBodyWriter;
    private MessageBodyReader<Object> messageBodyReader;
    private Type eventType;
    private Configuration configuration;
    private ObjectMapper objectMapper;
    private ByteBufferPool pool;
    private Marker marker;

    public SubscriberWebSocketServlet(String path,
                                      ReactiveEventStreams.Subscriber<T> subscriber,
                                      MessageBodyWriter<Object> messageBodyWriter,
                                      MessageBodyReader<Object> messageBodyReader,
                                      Type eventType,
                                      Configuration configuration,
                                      ObjectMapper objectMapper,
                                      ByteBufferPool pool,
                                      Marker marker) {

        this.path = path;
        this.subscriber = subscriber;
        this.messageBodyWriter = messageBodyWriter;
        this.messageBodyReader = messageBodyReader;
        this.eventType = eventType;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.pool = pool;
        this.marker = marker;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofSeconds(configuration.getLong("idle_timeout").orElse(-1L)));

        factory.addMapping(path, (jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
        {
            return new SubscriberWebSocketEndpoint<T>(jettyServerUpgradeRequest.getRequestPath(), subscriber, messageBodyWriter, messageBodyReader, objectMapper, eventType, pool, marker);
        });
    }
}
