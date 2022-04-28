package com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.service.log4jappender.log4j.DisruptorAppender;
import com.exoreaction.reactiveservices.service.log4jappender.resources.websocket.DisruptorWebSocketEndpoint;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class ReactiveStreamWebSocketServlet<T>
        extends JettyWebSocketServlet {
    private String path;
    private ReactiveEventStreams.Publisher<T> publisher;
    private EventHandler<Event<T>> serializer;


    public ReactiveStreamWebSocketServlet(String path,
                                          ReactiveEventStreams.Publisher<T> publisher,
                                          EventHandler<Event<T>> serializer) {

        this.path = path;
        this.publisher = publisher;
        this.serializer = serializer;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setMaxTextMessageSize(1048576);
        factory.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));

        factory.addMapping(path, (jettyServerUpgradeRequest, jettyServerUpgradeResponse) ->
        {
            Map<String, String> singleValueParameters = jettyServerUpgradeRequest
                    .getParameterMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().findFirst().orElse(null)));

            return new ReactiveStreamWebSocketEndpoint(publisher, singleValueParameters, serializer);
        });
    }
}
