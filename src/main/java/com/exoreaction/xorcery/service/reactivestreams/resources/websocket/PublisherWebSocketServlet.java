package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.Publisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.Marker;
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
    private MessageBodyWriter<Object> messageBodyWriter;
    private MessageBodyReader<Object> messageBodyReader;
    private Type resultType;
    private Configuration configuration;
    private ObjectMapper objectMapper;
    private ByteBufferPool pool;

    public PublisherWebSocketServlet(String path,
                                     Function<Configuration, Flow.Publisher<Object>> publisherFactory,
                                     MessageBodyWriter<Object> messageBodyWriter,
                                     MessageBodyReader<Object> messageBodyReader,
                                     Type resultType,
                                     Configuration configuration,
                                     ObjectMapper objectMapper,
                                     ByteBufferPool pool) {

        this.path = path;
        this.publisherFactory = publisherFactory;
        this.messageBodyWriter = messageBodyWriter;
        this.messageBodyReader = messageBodyReader;
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
                new PublisherWebSocketEndpoint(jettyServerUpgradeRequest.getRequestPath(), publisherFactory, messageBodyWriter, messageBodyReader, resultType, objectMapper, pool));
    }
}
