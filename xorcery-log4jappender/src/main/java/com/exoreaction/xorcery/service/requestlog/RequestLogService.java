package com.exoreaction.xorcery.service.requestlog;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.disruptor.handlers.BroadcastEventHandler;
import com.exoreaction.xorcery.jersey.AbstractFeature;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.conductor.helpers.ClientPublisherConductorListener;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.concurrent.Flow;

public class RequestLogService
        extends AbstractContainerLifecycleListener {

    public static final String SERVICE_TYPE = "requestlog";
    private final Disruptor<WithMetadata<ObjectNode>> disruptor;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void configure() {
            context.register(RequestLogService.class, ContainerLifecycleListener.class);
        }
    }

    private final ServiceResourceObject resourceObject;
    private final ReactiveStreams reactiveStreams;
    private final BroadcastEventHandler<WithMetadata<ObjectNode>> broadcastEventHandler = new BroadcastEventHandler<>(false);

    @Inject
    public RequestLogService(@Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                             ReactiveStreams reactiveStreams,
                             Conductor conductor,
                             Server server,
                             Configuration configuration) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;

        disruptor = new Disruptor<>(WithMetadata::new, 4096, new NamedThreadFactory("RequestLogPublisher-"));
        disruptor.handleEventsWith(broadcastEventHandler);

        JsonRequestLog requestLog = new JsonRequestLog(new LoggingMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build(), disruptor.getRingBuffer());
        server.setRequestLog(requestLog);

        conductor.addConductorListener(new ClientPublisherConductorListener(resourceObject.getServiceIdentifier(), cfg -> new RequestLogPublisher(), RequestLogPublisher.class, null, reactiveStreams));
    }

    @Override
    public void onStartup(Container container) {
        disruptor.start();

        resourceObject.getLinkByRel("requestlogevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new RequestLogPublisher(), RequestLogPublisher.class);
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        disruptor.shutdown();
    }

    public class RequestLogPublisher
            implements Flow.Publisher<WithMetadata<ObjectNode>> {
        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber) {
            subscriber.onSubscribe(broadcastEventHandler.add(subscriber));
        }
    }
}
