package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;

@Service
@Named("registry.upstream")
public class UpstreamSubscriber
        implements Flow.Subscriber<WithMetadata<ServerResourceDocument>> {
    private Topic<ServerResourceDocument> registryTopic;

    @Inject
    public UpstreamSubscriber(Topic<ServerResourceDocument> registryTopic,
                              Configuration configuration,
                              ReactiveStreams reactiveStreams) {
        this.registryTopic = registryTopic;
        configuration.getURI("registry.upstream.url").ifPresent(uri ->
        {
            reactiveStreams.subscribe(UriBuilder.fromUri(uri).path("ws/registry/publisher").build(), Configuration.empty(), this, getClass());
        });
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

    }

    @Override
    public void onNext(WithMetadata<ServerResourceDocument> server) {
        registryTopic.publish(server.event());
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
}
