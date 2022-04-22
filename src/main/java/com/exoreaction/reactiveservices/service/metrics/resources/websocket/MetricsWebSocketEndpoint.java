package com.exoreaction.reactiveservices.service.metrics.resources.websocket;

import com.codahale.metrics.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Semaphore;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class MetricsWebSocketEndpoint<T>
        implements WebSocketListener {
    private final MetricRegistry metricRegistry;
    private final Collection<String> metrics;
    private Session session;
    private Semaphore semaphore = new Semaphore(0);
    private ObjectMapper objectMapper = new ObjectMapper();

    public MetricsWebSocketEndpoint(MetricRegistry metricRegistry, Collection<String> metrics) {
        this.metricRegistry = metricRegistry;
        this.metrics = metrics.isEmpty() ? metricRegistry.getNames() : metrics;
    }

    // WebSocket
    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
    }

    @Override
    public void onWebSocketText(String message) {
        semaphore.release(Integer.parseInt(message));

        // Send metrics
        if (semaphore.drainPermits() > 0) {
            JsonObjectBuilder metricsBuilder = Json.createObjectBuilder();
            for (String metricName : metrics) {
                Metric metric = metricRegistry.getMetrics().get(metricName);
                try {
                    if (metric instanceof Gauge) {
                        Number value = (Number) ((Gauge<?>) metric).getValue();
                        if (value instanceof Double) {
                            if (Double.isNaN((Double) value))
                                continue; // Skip these
                        }
                        metricsBuilder.add(metricName, Json.createValue(value));
                    } else if (metric instanceof Meter) {
                        Meter meter = (Meter)metric;
                        JsonObjectBuilder builder = Json.createObjectBuilder();
                        builder.add("count", meter.getCount());
                        builder.add("meanrate", meter.getMeanRate());
                        metricsBuilder.add(metricName, builder.build());
                    } else if (metric instanceof Counter) {
                        Counter counter = (Counter)metric;
                        metricsBuilder.add(metricName, counter.getCount());
                    } else {
//                        System.out.println(metric.getClass());
                    }
                } catch (Throwable e) {
                    LogManager.getLogger(getClass()).error("Could not serialize metric " + metricName + "with value " + metric.toString(), e);
                }
            }

            try {
                JsonObject jsonObject = metricsBuilder.build();
                session.getRemote().sendBytes(ByteBuffer.wrap(objectMapper.writeValueAsBytes(Collections.emptyMap())));
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                Json.createWriter(out).write(jsonObject);
                session.getRemote().sendBytes(ByteBuffer.wrap(out.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        // Ignore
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
    }

    @Override
    public void onWebSocketError(Throwable cause) {
    }
}
