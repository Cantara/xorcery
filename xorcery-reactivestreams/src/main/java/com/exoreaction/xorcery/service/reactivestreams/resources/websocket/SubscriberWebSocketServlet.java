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

public class SubscriberWebSocketServlet
        extends JettyWebSocketServlet {
    private final String path;
    private final Function<Configuration, Flow.Subscriber<Object>> subscriberFactory;
    private final MessageWriter<Object> resultWriter;
    private final MessageReader<Object> eventReader;
    private final Configuration configuration;
    private final ObjectMapper objectMapper;
    private final ByteBufferPool pool;

    public SubscriberWebSocketServlet(String path,
                                      Function<Configuration, Flow.Subscriber<Object>> subscriberFactory,
                                      MessageWriter<Object> resultWriter,
                                      MessageReader<Object> eventReader,
                                      Configuration configuration,
                                      ObjectMapper objectMapper,
                                      ByteBufferPool pool) {

        this.path = path;
        this.subscriberFactory = subscriberFactory;
        this.resultWriter = resultWriter;
        this.eventReader = eventReader;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.pool = pool;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofSeconds(configuration.getLong("idle_timeout").orElse(-1L)));

        factory.addMapping(path, (jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
                new SubscriberWebSocketEndpoint(jettyServerUpgradeRequest.getRequestPath(), subscriberFactory, resultWriter, eventReader, objectMapper, pool));
    }
}
