package com.exoreaction.reactiveservices.service.metrics.resources.websocket;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.reactiveservices.service.log4jappender.log4j.DisruptorAppender;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class MetricsWebSocketServlet
    extends JettyWebSocketServlet
{
    private final MetricRegistry metricRegistry;

    @Inject
    public MetricsWebSocketServlet( MetricRegistry metricRegistry )
    {
        this.metricRegistry = metricRegistry;
    }

    @Override
    protected void configure( JettyWebSocketServletFactory factory )
    {
        factory.setMaxTextMessageSize( 1048576 );

        LoggerContext lc = (LoggerContext) LogManager.getContext( false );
        DisruptorAppender appender = lc.getConfiguration().getAppender( "Disruptor" );

        factory.addMapping( "/ws/metrics", (( jettyServerUpgradeRequest, jettyServerUpgradeResponse ) ->
        {
            List<String>
                metricNames = Arrays.asList(jettyServerUpgradeRequest.getParameterMap().get( "metrics" ).get( 0 ).split( "," ));
            return new MetricsWebSocketEndpoint( metricRegistry, metricNames );
        }) );
    }
}
