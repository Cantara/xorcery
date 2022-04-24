package com.exoreaction.reactiveservices.service.domainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.*;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.service.domainevents.disruptor.DomainEventSerializeEventHandler;
import com.exoreaction.reactiveservices.service.domainevents.resources.websocket.DomainEventsWebSocketServlet;
import com.exoreaction.reactiveservices.service.domainevents.api.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class DomainEventsService
        implements DomainEventPublisher, ContainerLifecycleListener {

    @Provider
    public static class Feature
            extends AbstractFeature {

        List<EventHandler<DomainEventHolder>> consumers = new CopyOnWriteArrayList<>();

        @Override
        public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server) {
            server.addService(new ResourceObject
                    .Builder("service", "domainevents")
                    .links(new Links.Builder().link("domainevents", server.getBaseUriBuilder().scheme("ws").path("ws/domainevents")))
                    .build());

            EventHandlerResult<DomainEvents, Metadata> eventHandlerResult = new EventHandlerResult<>();
            injectionManager.getInstance(ServletContextHandler.class).addServlet(new ServletHolder(new DomainEventsWebSocketServlet(consumers, eventHandlerResult)), "/ws/domainevents");

            context.register(new DomainEventsService(consumers, eventHandlerResult));

            return super.configure(context, injectionManager, server);
        }
    }

    private final Disruptor<DomainEventHolder> disruptor;

    public DomainEventsService(List<EventHandler<DomainEventHolder>> consumers, EventHandlerResult<DomainEvents, Metadata> eventHandlerResult) {
        disruptor =
                new Disruptor<>(DomainEventHolder::new, 4096, new NamedThreadFactory("DomainEventsDisruptor-"),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy());

        disruptor.handleEventsWith(
                        new MetadataSerializerEventHandler(),
                        new DomainEventSerializeEventHandler(new ObjectMapper()))
                .then(new UnicastEventHandler<>(consumers))
                .then(eventHandlerResult);
    }

    @Override
    public void onStartup(Container container) {
        disruptor.start();
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        disruptor.shutdown();
    }

    public CompletionStage<Metadata> publish(DomainEvents events) {
        CompletableFuture<Metadata> future = new CompletableFuture<>();
        disruptor.getRingBuffer().publishEvent((event, seq, e, f) ->
        {
            event.metadata.clear();
            // TODO populate metadata with system information
            event.event = e;
            event.result = f;
        }, events, future);

        return future;
    }
}
