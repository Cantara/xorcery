package com.exoreaction.reactiveservices.service.log4jappender.resources;

import com.exoreaction.reactiveservices.service.log4jappender.log4j.DisruptorAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class LogWebSocketServlet
    extends JettyWebSocketServlet
{
    public LogWebSocketServlet(  )
    {
    }

    @Override
    protected void configure( JettyWebSocketServletFactory factory )
    {
        factory.setMaxTextMessageSize( 1048576 );

        LoggerContext lc = (LoggerContext) LogManager.getContext( false );
        DisruptorAppender appender = lc.getConfiguration().getAppender( "Disruptor" );

        factory.addMapping( "/ws/logevents", (( jettyServerUpgradeRequest, jettyServerUpgradeResponse ) ->
        {
            return new DisruptorWebSocketEndpoint<>( appender.getConsumers() );
        }) );
    }
}
