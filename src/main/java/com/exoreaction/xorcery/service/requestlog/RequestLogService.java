package com.exoreaction.xorcery.service.requestlog;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.Xorcery;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

public class RequestLogService
        implements ContainerLifecycleListener, ReactiveEventStreams.Publisher<ObjectNode> {

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
                   Server server,
                   Configuration configuration) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;

        requestLog = new JsonRequestLog(new LoggingMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build());
        server.setRequestLog(requestLog);
    }

    @Override
    public void onStartup(Container container) {
        resourceObject.getLinkByRel("requestlogevents").ifPresent(link ->
        {
            reactiveStreams.publish(resourceObject.serviceIdentifier(), link, this);
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<ObjectNode> subscriber, Configuration parameters) {

        requestLog.setEventSink(subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
            @Override
            public void request(long n) {
                // Ignore
            }

            @Override
            public void cancel() {
                requestLog.setEventSink(null);
                subscriber.onComplete();
            }
        }));
    }
}
