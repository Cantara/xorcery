package com.exoreaction.reactiveservices.service.domainevents.resources.websocket;

import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvent;
import com.exoreaction.reactiveservices.service.log4jappender.resources.websocket.DisruptorWebSocketEndpoint;
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
