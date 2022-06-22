package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.Marker;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.lang.reflect.Type;
import java.time.Duration;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class PublisherWebSocketServlet<T>
        extends JettyWebSocketServlet {
    private String path;
    private ReactiveEventStreams.Publisher<T> publisher;
    private MessageBodyWriter<T> messageBodyWriter;
    private MessageBodyReader<Object> messageBodyReader;
    private Type resultType;
    private Configuration configuration;
    private ObjectMapper objectMapper;
    private Marker marker;

    public PublisherWebSocketServlet(String path,
                                     ReactiveEventStreams.Publisher<T> publisher,
                                     MessageBodyWriter<T> messageBodyWriter,
                                     MessageBodyReader<Object> messageBodyReader,
                                     Type resultType,
                                     Configuration configuration,
                                     ObjectMapper objectMapper, Marker marker) {

        this.path = path;
        this.publisher = publisher;
        this.messageBodyWriter = messageBodyWriter;
        this.messageBodyReader = messageBodyReader;
        this.resultType = resultType;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.marker = marker;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofMillis(configuration.getLong("idle_timeout").orElse(-1L)));

        factory.addMapping(path, (jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
        {
            return new PublisherWebSocketEndpoint<T>(jettyServerUpgradeRequest.getRequestPath(), publisher, messageBodyWriter, messageBodyReader, resultType, objectMapper, marker);
        });
    }
}
