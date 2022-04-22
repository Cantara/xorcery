package com.exoreaction.reactiveservices.service.domainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.BroadcastEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.disruptor.MetadataSerializerEventHandler;
import com.exoreaction.reactiveservices.disruptor.UnicastEventHandler;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.domainevents.resources.DomainEventsWebSocketServlet;
import com.exoreaction.reactiveservices.service.domainevents.spi.DomainEvent;
import com.exoreaction.reactiveservices.service.domainevents.spi.Metadata;
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
    implements ContainerLifecycleListener
{

    @Provider
    public static class Feature
            extends AbstractFeature {

        List<EventHandler<EventHolder<List<DomainEvent>>>> consumers = new CopyOnWriteArrayList<>();

        @Override
        public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server)
        {
            server.addService(new ResourceObject.Builder("service", "domainevents")
                    .links(
                            new Links.Builder().link("domainevents", URI.create("ws://localhost:8080/ws/domainevents")).build())
                    .build());
            injectionManager.getInstance(ServletContextHandler.class).addServlet(new ServletHolder(new DomainEventsWebSocketServlet(consumers)), "/ws/domainevents");

            context.register(new DomainEventsService(consumers));

            return super.configure(context, injectionManager, server);
        }
    }
    private final Disruptor<EventHolder<List<DomainEvent>>> disruptor;

    public DomainEventsService(List<EventHandler<EventHolder<List<DomainEvent>>>> consumers)
    {
        disruptor =
                new Disruptor<>( EventHolder::new, 4096, new NamedThreadFactory( "DomainEventsDisruptor-" ),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy() );

        disruptor.handleEventsWith(
                        new MetadataSerializerEventHandler(),
                        new DomainEventSerializeEventHandler( new ObjectMapper()) )
                .then( new UnicastEventHandler<>( consumers ) );

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

    public CompletionStage<Metadata> publish(Metadata metadata, List<DomainEvent> events)
    {
        disruptor.getRingBuffer().publishEvent((event, seq, md, e)->
        {
            event.metadata.clear();
            event.metadata.putAllValues(metadata.getMetadata());
            event.event = events;
        }, metadata, events);

        // TODO Wait for write confirmation
        return CompletableFuture.completedStage(metadata);
    }
}
