package com.exoreaction.reactiveservices.service.loggingconsumer;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.BroadcastEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.disruptor.MetadataDeserializerEventHandler;
import com.exoreaction.reactiveservices.disruptor.WebSocketFlowControlEventHandler;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.service.loggingconsumer.disruptor.Log4jDeserializeEventHandler;
import com.exoreaction.reactiveservices.service.registry.client.RegistryClient;
import com.exoreaction.reactiveservices.service.registry.client.RegistryListener;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
@Contract
public class LoggingConsumerService
        implements ContainerLifecycleListener {

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server) {
            if (injectionManager.getInstance(Configuration.class).getBoolean("loggingconsumer.enabled", true)) {
                server.addService(new ResourceObject.Builder("service", "loggingconsumer").build());

                context.register(LoggingConsumerService.class, LoggingConsumerService.class, ContainerLifecycleListener.class);
            }

            return super.configure(context, injectionManager, server);
        }
    }

    private RegistryClient registryClient;
    private List<EventHandler<EventHolder<LogEvent>>> handlers = new CopyOnWriteArrayList<>();

    @Inject
    public LoggingConsumerService(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public void onStartup(Container container) {
        System.out.println("Log consumer Startup");

        registryClient.addRegistryListener(new LoggingRegistryListener());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

        // TODO Close active sessions
        System.out.println("Shutdown");
    }

    public void addLogHandler(EventHandler<EventHolder<LogEvent>> handler) {
        handlers.add(handler);
    }

    private void connect(Link logSource) {
        Disruptor<EventHolder<LogEvent>> disruptor =
                new Disruptor<>(EventHolder::new, 4096, new NamedThreadFactory("SysoutDisruptorIn-"),
                        ProducerType.SINGLE,
                        new BlockingWaitStrategy());

        registryClient.connect(logSource.getHref(), new LoggingClientEndpoint(disruptor))
                .thenAccept(session ->
                {
                    try {
                        disruptor.handleEventsWith(new MetadataDeserializerEventHandler(),
                                        new Log4jDeserializeEventHandler(new JsonLogEventParser()))
                                .then(new BroadcastEventHandler<>(handlers), new WebSocketFlowControlEventHandler(4096, session, Executors.newSingleThreadExecutor()));
                        disruptor.start();

                        System.out.println("Receiving log messages from " + logSource.getHref());
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }).exceptionally(e ->
                {
                    e.printStackTrace();
                    return null;
                });
    }

    private class LoggingRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {
            service.getLinks().getRel("logevents").ifPresent(LoggingConsumerService.this::connect);
        }
    }

    private class LoggingClientEndpoint
            implements WebSocketListener {
        private final Disruptor<EventHolder<LogEvent>> disruptor;

        ByteBuffer headers;

        private LoggingClientEndpoint(
                Disruptor<EventHolder<LogEvent>> disruptor) {
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
