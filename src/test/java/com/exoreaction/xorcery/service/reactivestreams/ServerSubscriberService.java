package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

@Contract
public class ServerSubscriberService
        implements ContainerLifecycleListener,
        ReactiveEventStreams.Subscriber<String> {
    private ReactiveStreams reactiveStreams;
    private ServiceResourceObject serviceResourceObject;

    public static final String SUBSCRIBER_REL = "eventsubscriber";

    @Provider
    public static class ServerFeature
            extends AbstractFeature {

        public static final String SERVICE_TYPE = "eventserver";

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.websocket(ServerSubscriberService.SUBSCRIBER_REL, "ws/eventsubscriber");
        }

        @Override
        protected void configure() {
            context.register(ServerSubscriberService.class, ServerSubscriberService.class, ContainerLifecycleListener.class);
        }
    }

    @Inject
    public ServerSubscriberService(ReactiveStreams reactiveStreams,
                                   @Named(ServerFeature.SERVICE_TYPE) ServiceResourceObject serviceResourceObject) {
        this.reactiveStreams = reactiveStreams;
        this.serviceResourceObject = serviceResourceObject;
    }

    @Override
    public void onStartup(Container container) {
        reactiveStreams.subscriber(serviceResourceObject.serviceIdentifier(), serviceResourceObject.getLinkByRel(SUBSCRIBER_REL).orElseThrow(), this);
        LogManager.getLogger(getClass()).info("Startup");
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    @Override
    public EventSink<Event<String>> onSubscribe(ReactiveEventStreams.Subscription subscription, Configuration configuration) {
        Disruptor<Event<String>> disruptor = new Disruptor<>(Event::new, 1024, new NamedThreadFactory("ServerSubscriber-"));
        disruptor.handleEventsWith(new SubscriberEventHandler(subscription));
        disruptor.start();

        subscription.request(1024);

        return disruptor.getRingBuffer();
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println(throwable);
    }

    @Override
    public void onComplete() {
        System.out.println("Done!");
    }

    public class SubscriberEventHandler
            implements EventHandler<Event<String>>
    {
        private ReactiveEventStreams.Subscription subscription;
        private long batchSize;

        public SubscriberEventHandler(ReactiveEventStreams.Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onEvent(Event<String> event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println(event.event);

            if (endOfBatch)
                subscription.request(batchSize);
        }

        @Override
        public void onBatchStart(long batchSize) {
            this.batchSize = batchSize;
        }
    }
}
