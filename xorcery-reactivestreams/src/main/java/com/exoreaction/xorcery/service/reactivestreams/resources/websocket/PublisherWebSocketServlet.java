package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

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
    private MessageWriter<Object> eventWriter;
    private MessageReader<Object> resultReader;
    private Configuration configuration;
    private ObjectMapper objectMapper;
    private ByteBufferPool pool;

    public PublisherWebSocketServlet(String path,
                                     Function<Configuration, Flow.Publisher<Object>> publisherFactory,
                                     MessageWriter<Object> eventWriter,
                                     MessageReader<Object> resultReader,
                                     Configuration configuration,
                                     ObjectMapper objectMapper,
                                     ByteBufferPool pool) {

        this.path = path;
        this.publisherFactory = publisherFactory;
        this.eventWriter = eventWriter;
        this.resultReader = resultReader;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.pool = pool;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofSeconds(configuration.getLong("idle_timeout").orElse(-1L)));

        factory.addMapping(path, (jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
                new PublisherWebSocketEndpoint(jettyServerUpgradeRequest.getRequestPath(), publisherFactory, eventWriter, resultReader, objectMapper, pool));
    }
}
