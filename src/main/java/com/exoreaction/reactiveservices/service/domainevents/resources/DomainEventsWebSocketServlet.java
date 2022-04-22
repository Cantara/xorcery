package com.exoreaction.reactiveservices.service.domainevents.resources;

import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.service.domainevents.spi.DomainEvent;
import com.exoreaction.reactiveservices.service.log4jappender.log4j.DisruptorAppender;
import com.exoreaction.reactiveservices.service.log4jappender.resources.DisruptorWebSocketEndpoint;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.time.Duration;
import java.util.List;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class DomainEventsWebSocketServlet
    extends JettyWebSocketServlet
{
    private List<EventHandler<EventHolder<List<DomainEvent>>>> consumers;

    public DomainEventsWebSocketServlet(List<EventHandler<EventHolder<List<DomainEvent>>>> consumers)
    {
        this.consumers = consumers;
    }

    @Override
    protected void configure( JettyWebSocketServletFactory factory )
    {
        factory.setMaxTextMessageSize( 1048576 );
        factory.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));

        factory.addMapping( "/ws/domainevents", (( jettyServerUpgradeRequest, jettyServerUpgradeResponse ) ->
        {
            return new DisruptorWebSocketEndpoint<>( consumers );
        }) );
    }
}
