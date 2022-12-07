package com.exoreaction.xorcery.service.reactivestreams.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class PublishersReactiveStreamsServlet
        extends JettyWebSocketServlet {
    private final Configuration configuration;
    private final Function<String, Object> publisherWebSocketEndpointFactory;

    public PublishersReactiveStreamsServlet(Configuration configuration,
                                            Function<String, Object> publisherWebSocketEndpointFactory) {

        this.publisherWebSocketEndpointFactory = publisherWebSocketEndpointFactory;
        this.configuration = configuration;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofSeconds(configuration.getLong("idle_timeout").orElse(-1L)));

        factory.setCreator((jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
                publisherWebSocketEndpointFactory.apply(jettyServerUpgradeRequest.getRequestPath().substring( "/streams/publishers/".length())));
    }
}
