package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class PublisherWebSocketServlet
        extends JettyWebSocketServlet {
    private String path;
    private Function<Configuration, Flow.Publisher<Object>> publisherFactory;
    private MessageBodyWriter<Object> eventWriter;
    private MessageBodyReader<Object> resultReader;
    private Type eventType;
    private Type resultType;
    private Configuration configuration;
    private ObjectMapper objectMapper;
    private ByteBufferPool pool;

    public PublisherWebSocketServlet(String path,
                                     Function<Configuration, Flow.Publisher<Object>> publisherFactory,
                                     MessageBodyWriter<Object> eventWriter,
                                     MessageBodyReader<Object> resultReader,
                                     Type eventType,
                                     Type resultType,
                                     Configuration configuration,
                                     ObjectMapper objectMapper,
                                     ByteBufferPool pool) {

        this.path = path;
        this.publisherFactory = publisherFactory;
        this.eventWriter = eventWriter;
        this.resultReader = resultReader;
        this.eventType = eventType;
        this.resultType = resultType;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.pool = pool;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofSeconds(configuration.getLong("idle_timeout").orElse(-1L)));

        factory.addMapping(path, (jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
                new PublisherWebSocketEndpoint(jettyServerUpgradeRequest.getRequestPath(), publisherFactory, eventWriter, resultReader, eventType, resultType, objectMapper, pool));
    }
}
