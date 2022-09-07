package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams2;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.Flow;

@Contract
public class ServerSubscriberService
        implements ContainerLifecycleListener,
        Flow.Subscriber<String> {
    private ReactiveStreams2 reactiveStreams;
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
    public ServerSubscriberService(ReactiveStreams2 reactiveStreams,
                                   @Named(ServerFeature.SERVICE_TYPE) ServiceResourceObject serviceResourceObject) {
        this.reactiveStreams = reactiveStreams;
        this.serviceResourceObject = serviceResourceObject;
    }

    @Override
    public void onStartup(Container container) {
        reactiveStreams.subscriber(serviceResourceObject.getLinkByRel(SUBSCRIBER_REL).orElseThrow().getHrefAsUri().getPath(), cfg -> this);
        LogManager.getLogger(getClass()).info("Startup");
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

    }

    @Override
    public void onNext(String item) {
        System.out.println(item);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
}
