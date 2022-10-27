package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.JsonApiRels;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InstanceLifecycleEvent;
import org.glassfish.hk2.api.InstanceLifecycleEventType;
import org.glassfish.hk2.api.InstanceLifecycleListener;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

@Service
@MessageReceiver
@Named("registry.upstream")
public class UpstreamPublisher
        implements Flow.Publisher<WithMetadata<ServerResourceDocument>>, InstanceLifecycleListener {
    private final Provider<Configuration> configuration;
    private final Provider<ReactiveStreams> reactiveStreams;
    private final List<ServiceResourceObject> serviceResources = new ArrayList<>();

    private Flow.Subscriber<? super WithMetadata<ServerResourceDocument>> subscriber;

    @Inject
    public UpstreamPublisher(Provider<Configuration> configuration,
                             Provider<ReactiveStreams> reactiveStreams) {
        this.configuration = configuration;
        this.reactiveStreams = reactiveStreams;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<ServerResourceDocument>> subscriber) {
        this.subscriber = subscriber;
    }

    public void service(@SubscribeTo ServiceResourceObject serviceResourceObject) {
        serviceResources.add(serviceResourceObject);
    }

    @Override
    public Filter getFilter() {
        return d -> d.getImplementation().equals("com.exoreaction.xorcery.core.Xorcery");
    }

    @Override
    public void lifecycleEvent(InstanceLifecycleEvent lifecycleEvent) {
        if (lifecycleEvent.getEventType().equals(InstanceLifecycleEventType.POST_PRODUCTION) &&
                subscriber != null) {

            StandardConfiguration standardConfiguration = configuration::get;

            standardConfiguration.context().getURI("registry.upstream.url").ifPresent(uri ->
            {
                reactiveStreams.get().publish(UriBuilder.fromUri(uri).path("ws/registrysubscriber").build(), Configuration.empty(), this, getClass());
            });

            ResourceObjects.Builder builder = new ResourceObjects.Builder();
            for (ServiceResourceObject serviceResource : serviceResources) {
                builder.resource(serviceResource.resourceObject());
            }
            ResourceDocument serverDocument = new ResourceDocument.Builder()
                    .links(new Links.Builder().link(JsonApiRels.self, standardConfiguration.getServerUri()).build())
                    .data(builder.build())
                    .build();
            ServerResourceDocument serverResourceDocument = new ServerResourceDocument(serverDocument);
            // TODO Probably change this to use a CompletableFuture instead, because async?
            subscriber.onNext(new WithMetadata<>(new Metadata.Builder().build(), serverResourceDocument));
            subscriber.onComplete();
        }
    }
}
