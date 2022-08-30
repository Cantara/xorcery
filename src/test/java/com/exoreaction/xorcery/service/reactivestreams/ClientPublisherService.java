package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.lmax.disruptor.EventSink;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static com.exoreaction.xorcery.service.reactivestreams.ClientPublisherService.ClientFeature.SERVICE_TYPE;

@Contract
@Singleton
public class ClientPublisherService
        implements ContainerLifecycleListener, ReactiveEventStreams.Publisher<String> {
    private Conductor conductor;

    @Provider
    public static class ClientFeature
            extends AbstractFeature {

        public static final String SERVICE_TYPE = "eventclient";

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void configure() {
            context.register(ClientPublisherService.class, ClientPublisherService.class, ContainerLifecycleListener.class);
        }
    }

    private final CompletableFuture<Void> done = new CompletableFuture<>();

    @Inject
    public ClientPublisherService(@Named(SERVICE_TYPE) ServiceResourceObject sro,
                                  Conductor conductor,
                                  ReactiveStreams reactiveStreams) {
        conductor.addConductorListener(new ClientPublisherConductorListener<>(sro.serviceIdentifier(), this, ServerSubscriberService.SUBSCRIBER_REL, reactiveStreams));
    }

    @Override
    public void onStartup(Container container) {
        LogManager.getLogger(getClass()).info("Startup");
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public CompletableFuture<Void> getDone() {
        return done;
    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<String> subscriber, Configuration parameters) {

        Semaphore semaphore = new Semaphore(0);
        AtomicLong initialRequest = new AtomicLong(-1);

        EventSink<Event<String>> eventSink = subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
            @Override
            public void request(long n) {
                semaphore.release((int) n);
                if (initialRequest.get() == -1)
                {
                    initialRequest.set(n);
                }
            }

            @Override
            public void cancel() {

            }
        }, parameters);

        CompletableFuture.runAsync(() ->
        {
            Metadata metadata = new Metadata.Builder().build();

            try {
                for (int i = 0; i < 100; i++) {
                    semaphore.acquire();
                    eventSink.publishEvent((event, sequence, e, m) ->
                            {
                                event.event = e;
                                event.metadata = m;
                            }, i + "", metadata);
                }

                semaphore.acquire((int)initialRequest.get());
                LogManager.getLogger(getClass()).info("All events published, and requests back to original value. Done!");
                subscriber.onComplete();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((r, t) -> done.complete(null));
    }
}
