package com.exoreaction.reactiveservices.service.metrics.resources.websocket;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.Semaphore;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class MetricsWebSocketEndpoint<T>
    implements WebSocketListener
{
    private final MetricRegistry metricRegistry;
    private final Collection<String> metrics;
    private Session session;
    private Semaphore semaphore = new Semaphore(0);

    public MetricsWebSocketEndpoint( MetricRegistry metricRegistry, Collection<String> metrics )
    {
        this.metricRegistry = metricRegistry;
        this.metrics = metrics;
    }

    // WebSocket
    @Override
    public void onWebSocketBinary( byte[] payload, int offset, int len )
    {
    }

    @Override
    public void onWebSocketText( String message )
    {
        semaphore.release(Integer.parseInt( message ));

        // Send metrics
        if (semaphore.drainPermits() > 0)
        {
            JsonObjectBuilder metricsBuilder = Json.createObjectBuilder();
            for ( String metricName : metrics )
            {
                Metric metric = metricRegistry.getMetrics().get( metricName );
                if (metric instanceof Gauge )
                {
                    metricsBuilder.add(metricName, (long)((Gauge<?>) metric).getValue());
                }
            }

            try
            {
                session.getRemote().sendBytes( ByteBuffer.wrap(new ObjectMapper().writeValueAsBytes( metricsBuilder.build() )) );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onWebSocketClose( int statusCode, String reason )
    {
        // Ignore
    }

    @Override
    public void onWebSocketConnect( Session session )
    {
        this.session = session;
    }

    @Override
    public void onWebSocketError( Throwable cause )
    {
    }
}
