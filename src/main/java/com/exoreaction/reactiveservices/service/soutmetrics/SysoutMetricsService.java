package com.exoreaction.reactiveservices.service.soutmetrics;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.disruptor.MetadataDeserializerEventHandler;
import com.exoreaction.reactiveservices.disruptor.WebSocketFlowControlEventHandler;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.configuration.Configuration;
import com.exoreaction.reactiveservices.service.registry.client.RegistryClient;
import com.exoreaction.reactiveservices.service.registry.client.RegistryListener;
import com.exoreaction.reactiveservices.service.soutmetrics.disruptor.MetricDeserializeEventHandler;
import com.exoreaction.reactiveservices.service.soutmetrics.disruptor.SysoutMetricEventHandler;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class SysoutMetricsService
        implements Closeable, ContainerLifecycleListener {

    private ScheduledExecutorService scheduledExecutorService;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server)
        {
            if (injectionManager.getInstance(Configuration.class).getConfiguration("soutmetrics").getBoolean("enabled", true))
            {
                server.addService(new ResourceObject.Builder("service", "sysoutmetrics").build());

                context.register(SysoutMetricsService.class);
            }

            return super.configure(context, injectionManager, server);
        }
    }

    private RegistryClient registryClient;

    @Inject
    public SysoutMetricsService(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public void onStartup(Container container) {
        System.out.println("Sysoutmetrics Startup");

        registryClient.addRegistryListener(new MetricsRegistryListener());
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onReload(Container container) {

    }
    @Override
    public void onShutdown(Container container) {
        System.out.println("Shutdown");
        scheduledExecutorService.shutdown();
    }

    public void connect(Link metricSource) {
        Disruptor<EventHolder<JsonObject>> disruptor =
                new Disruptor<>(EventHolder::new, 4096, new NamedThreadFactory("SysoutMetricsDisruptorIn-"),
                        ProducerType.SINGLE,
                        new BlockingWaitStrategy());

        registryClient.connect(metricSource.getHrefAsUriTemplate().createURI(""), new MetricsClientEndpoint(disruptor))
                .thenAccept(session ->
                {
                    try {
                        disruptor.handleEventsWith(new MetadataDeserializerEventHandler(),
                                        new MetricDeserializeEventHandler())
                                .then(new SysoutMetricEventHandler(), new WebSocketFlowControlEventHandler(1, session, runnable ->
                                {
                                    scheduledExecutorService.schedule(runnable, 5, TimeUnit.SECONDS);
                                }));
                        disruptor.start();

                        System.out.println("Receiving metric messages from " + metricSource.getHref());
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }).exceptionally(e ->
                {
                    e.printStackTrace();
                    return null;
                });
    }

    @Override
    public void close() throws IOException {
        // TODO Close active sessions
    }

    private class MetricsRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {
            service.getLinks().getRel("metricevents").ifPresent(SysoutMetricsService.this::connect);
        }
    }

    private class MetricsClientEndpoint
            implements WebSocketListener {
        private final Disruptor<EventHolder<JsonObject>> disruptor;

        ByteBuffer headers;

        private MetricsClientEndpoint(
                Disruptor<EventHolder<JsonObject>> disruptor) {
            this.disruptor = disruptor;
        }

        @Override
        public void onWebSocketText(String message) {
            System.out.println("Text:" + message);
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int len) {
            if (headers == null) {
                headers = ByteBuffer.wrap(payload, offset, len);
            } else {
                ByteBuffer body = ByteBuffer.wrap(payload, offset, len);
                disruptor.publishEvent((holder, seq, h, b) ->
                {
                    holder.headers = h;
                    holder.body = b;
                }, headers, body);
                headers = null;
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            System.out.printf("Close:%d %s%n", statusCode, reason);
            disruptor.shutdown();
        }

        @Override
        public void onWebSocketConnect(Session session) {
            System.out.printf("Connect:%s%n", session.getRemote().getRemoteAddress().toString());
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            StringWriter sw = new StringWriter();
            cause.printStackTrace(new PrintWriter(sw));

            System.out.printf("Error:%s%n", sw.toString());
        }
    }
}
