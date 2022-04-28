package com.exoreaction.reactiveservices.service.domainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.*;
import com.exoreaction.reactiveservices.disruptor.handlers.MetadataSerializerEventHandler;
import com.exoreaction.reactiveservices.disruptor.handlers.UnicastEventHandler;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.disruptor.handlers.ObjectMapperSerializeEventHandler;
import com.exoreaction.reactiveservices.service.domainevents.resources.websocket.DomainEventsWebSocketServlet;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.service.helpers.ServiceResourceObjectBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class DomainEventsPublisher
        implements DomainEventPublisher, ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "domainevents";

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObjectBuilder builder) {
                    builder.websocket("domainevents", "ws/domainevents");
        }

        @Override
        protected void configure() {
            EventHandlerResult<DomainEvents, Metadata> eventHandlerResult = new EventHandlerResult<>();
            injectionManager.getInstance(ServletContextHandler.class).addServlet(new ServletHolder(new DomainEventsWebSocketServlet(consumers, eventHandlerResult)), "/ws/domainevents");

            context.register(new DomainEventsPublisher(consumers, eventHandlerResult));
        }

        List<EventHandler<DomainEventHolder>> consumers = new CopyOnWriteArrayList<>();
    }

    private final Disruptor<DomainEventHolder> disruptor;

    public DomainEventsPublisher(List<EventHandler<DomainEventHolder>> consumers, EventHandlerResult<DomainEvents, Metadata> eventHandlerResult) {
        disruptor =
                new Disruptor<>(DomainEventHolder::new, 4096, new NamedThreadFactory("DomainEventsDisruptor-"),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy());

        disruptor.handleEventsWith(
                        new MetadataSerializerEventHandler(),
                        new ObjectMapperSerializeEventHandler(new ObjectMapper()))
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
