package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

@Service
@Named("registry.publisher")
public class RegistryUpstreamService {

    private final List<Flow.Subscriber<? super WithMetadata<ServerResourceDocument>>> subscribers = new CopyOnWriteArrayList<>();
    private final Map<String, WithMetadata<ServerResourceDocument>> servers = new ConcurrentHashMap<>();
    private Topic<ServerResourceDocument> serverResourceDocumentTopic;

    @Inject
    public RegistryUpstreamService(ReactiveStreams reactiveStreams,
                                   Topic<ServerResourceDocument> serverResourceDocumentTopic) {
        this.serverResourceDocumentTopic = serverResourceDocumentTopic;
        reactiveStreams.publisher("/ws/registrypublisher", RegistryPublisher::new, RegistryPublisher.class);
        reactiveStreams.subscriber("/ws/registrysubscriber", RegistrySubscriber::new, RegistrySubscriber.class);
    }

    private class RegistryPublisher
            implements Flow.Publisher<WithMetadata<ServerResourceDocument>> {

        public RegistryPublisher(Configuration configuration) {
        }

        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<ServerResourceDocument>> subscriber) {

            Iterator<WithMetadata<ServerResourceDocument>> serverIterator = servers.values().iterator();

            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    for (int i = 0; i < n; i++) {
                        if (serverIterator.hasNext())
                            subscriber.onNext(serverIterator.next());
                    }
                }

                @Override
                public void cancel() {
                    subscribers.remove(subscriber);
                    subscriber.onComplete();
                }
            });

            subscribers.add(subscriber);
        }
    }

    private class RegistrySubscriber
            implements Flow.Subscriber<WithMetadata<ServerResourceDocument>> {

        public RegistrySubscriber(Configuration configuration) {
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
        }

        @Override
        public void onNext(WithMetadata<ServerResourceDocument> item) {
            servers.put(item.event().getSelf().getHref(), item);
            for (Flow.Subscriber<? super WithMetadata<ServerResourceDocument>> subscriber : subscribers) {
                subscriber.onNext(item);
            }
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }
}
