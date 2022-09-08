package com.exoreaction.xorcery.service.requestlog;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.helper.ClientPublisherConductorListener;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.concurrent.Flow;

public class RequestLogService
        extends AbstractContainerLifecycleListener
        implements Flow.Publisher<WithMetadata<ObjectNode>> {

    public static final String SERVICE_TYPE = "requestlog";

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.websocket("requestlogevents", "ws/requestlogevents");
        }

        @Override
        protected void configure() {
            context.register(RequestLogService.class, ContainerLifecycleListener.class);
        }
    }

    private final ServiceResourceObject resourceObject;
    private final ReactiveStreams reactiveStreams;
    private final JsonRequestLog requestLog;

    @Inject
    public RequestLogService(@Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                             ReactiveStreams reactiveStreams,
                             Conductor conductor,
                             Server server,
                             Configuration configuration) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;

        requestLog = new JsonRequestLog(new LoggingMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build());
        server.setRequestLog(requestLog);

        conductor.addConductorListener(new ClientPublisherConductorListener(resourceObject.serviceIdentifier(), cfg -> this, RequestLogService.class, "opensearch", reactiveStreams));
    }

    @Override
    public void onStartup(Container container) {
        resourceObject.getLinkByRel("requestlogevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> this, RequestLogService.class);
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
    }

    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber) {
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                // Ignore
            }

            @Override
            public void cancel() {
                requestLog.setSubscriber(null);
                subscriber.onComplete();
            }
        });
        requestLog.setSubscriber(subscriber);
    }
}
