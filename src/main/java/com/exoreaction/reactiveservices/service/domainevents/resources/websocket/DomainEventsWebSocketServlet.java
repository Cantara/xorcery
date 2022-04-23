package com.exoreaction.reactiveservices.service.domainevents.resources.websocket;

import com.exoreaction.reactiveservices.disruptor.EventHandlerResult;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.service.domainevents.DomainEventHolder;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvent;
import com.exoreaction.reactiveservices.service.domainevents.api.Metadata;
import com.lmax.disruptor.EventHandler;
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
    private List<EventHandler<DomainEventHolder>> consumers;
    private EventHandlerResult<List<DomainEvent>, Metadata> eventHandlerResult;

    public DomainEventsWebSocketServlet(List<EventHandler<DomainEventHolder>> consumers,
                                        EventHandlerResult<List<DomainEvent>, Metadata> eventHandlerResult)
    {
        this.consumers = consumers;
        this.eventHandlerResult = eventHandlerResult;
    }

    @Override
    protected void configure( JettyWebSocketServletFactory factory )
    {
        factory.setMaxTextMessageSize( 1048576 );
        factory.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));

        factory.addMapping( "/ws/domainevents", (( jettyServerUpgradeRequest, jettyServerUpgradeResponse ) ->
        {
            return new DomainEventsWebSocketEndpoint( consumers, eventHandlerResult );
        }) );
    }
}
