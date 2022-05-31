package com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket;

import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
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
    private ObjectMapper objectMapper;

    public PublisherWebSocketServlet(String path,
                                     ReactiveEventStreams.Publisher<T> publisher,
                                     MessageBodyWriter<T> messageBodyWriter,
                                     MessageBodyReader<Object> messageBodyReader,
                                     Type resultType,
                                     ObjectMapper objectMapper) {

        this.path = path;
        this.publisher = publisher;
        this.messageBodyWriter = messageBodyWriter;
        this.messageBodyReader = messageBodyReader;
        this.resultType = resultType;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));

        factory.addMapping(path, (jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
        {
            ObjectNode parameters = objectMapper.valueToTree(jettyServerUpgradeRequest.getParameterMap());
            return new PublisherWebSocketEndpoint<T>(jettyServerUpgradeRequest.getRequestPath(), publisher, parameters, messageBodyWriter, messageBodyReader, resultType, objectMapper);
        });
    }
}
