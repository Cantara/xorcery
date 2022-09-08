package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.StandardConfiguration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.server.Xorcery;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class ClientPublisherServerSubscriberIT {

    private static final String config = """
            reactivestreams.enabled: true
            """;

    @Test
    public void givenSubscriberWhenPublishEventsThenSubscriberConsumesEvents2() throws Exception {
        try (Xorcery xorcery = new Xorcery(Configuration.Builder.loadTest(null).addYaml(config).build())) {
            ReactiveStreams reactiveStreams = xorcery.getInjectionManager().getInstance(ReactiveStreams.class);

            // Server subscriber
            reactiveStreams.subscriber("/serversubscriber", cfg -> new StringServerSubscriber(), StringServerSubscriber.class);

            // Client publisher
            final StringClientPublisher clientPublisher = new StringClientPublisher();
            URI serverUri = new StandardConfiguration.Impl(xorcery.getInjectionManager().getInstance(Configuration.class)).getServerUri();
            reactiveStreams.publish(UriBuilder.fromUri(serverUri).scheme("ws").path("serversubscriber").build(), Configuration.empty(), clientPublisher, clientPublisher.getClass());

            clientPublisher.getDone().get();

            System.out.println("DONE!");
        }
    }

    @Test
    public void givenSubscriberWhenPublishEventsWithResultsThenSubscriberCalculatesResults() throws Exception {
        try (Xorcery xorcery = new Xorcery(Configuration.Builder.loadTest(null).addYaml(config).build())) {
            ReactiveStreams reactiveStreams = xorcery.getInjectionManager().getInstance(ReactiveStreams.class);

            // Server subscriber
            ServerSubscriber<WithResult<String, Integer>> serverSubscriber = new ServerSubscriber<>() {
                @Override
                public void onNext(WithResult<String, Integer> item) {
                    item.result().complete(item.event().length());
                    subscription.request(1);
                }
            };

            reactiveStreams.subscriber("/serversubscriber", cfg -> serverSubscriber, (Class<? extends Flow.Subscriber<?>>) serverSubscriber.getClass());

            // Client publisher
            final ClientPublisher<WithResult<String, Integer>> clientPublisher = new ClientPublisher<>() {
                @Override
                protected void publish(Flow.Subscriber<? super WithResult<String, Integer>> subscriber) {
                    for (int i = 0; i < 100; i++) {
                        CompletableFuture<Integer> future = new CompletableFuture<>();
                        future.whenComplete((r, t)->
                                {
                                    System.out.println("Length:"+r);
                                });
                        subscriber.onNext(new WithResult<>(i + "", future));
                    }
                    subscriber.onComplete();
                }
            };

            URI serverUri = new StandardConfiguration.Impl(xorcery.getInjectionManager().getInstance(Configuration.class)).getServerUri();
            reactiveStreams.publish(UriBuilder.fromUri(serverUri).scheme("ws").path("serversubscriber").build(), Configuration.empty(), clientPublisher, (Class<? extends Flow.Publisher<?>>) clientPublisher.getClass());

            clientPublisher.getDone().get();

            System.out.println("DONE!");
        }
    }

    @Test
    public void givenSubscriberWhenPublishEventsWithResultsAndMetadataThenSubscriberCalculatesResults() throws Exception {
        try (Xorcery xorcery = new Xorcery(Configuration.Builder.loadTest(null).addYaml(config).build())) {
            ReactiveStreams reactiveStreams = xorcery.getInjectionManager().getInstance(ReactiveStreams.class);

            // Server subscriber
            ServerSubscriber<WithResult<WithMetadata<String>, Integer>> serverSubscriber = new ServerSubscriber<>() {
                @Override
                public void onNext(WithResult<WithMetadata<String>, Integer> item) {
                    item.result().complete(item.event().event().length());
                    subscription.request(1);
                }
            };

            reactiveStreams.subscriber("/serversubscriber", cfg -> serverSubscriber, (Class<? extends Flow.Subscriber<?>>) serverSubscriber.getClass());

            // Client publisher
            final ClientPublisher<WithResult<WithMetadata<String>, Integer>> clientPublisher = new ClientPublisher<>() {
                @Override
                protected void publish(Flow.Subscriber<? super WithResult<WithMetadata<String>, Integer>> subscriber) {
                    for (int i = 0; i < 100; i++) {
                        CompletableFuture<Integer> future = new CompletableFuture<>();
                        future.whenComplete((r, t)->
                                {
                                    System.out.println("Length:"+r);
                                });
                        subscriber.onNext(new WithResult<>(new WithMetadata<>(new Metadata.Builder()
                                .add("foo", "bar")
                                .build(), i + "XORCERY"), future));
                    }
                    subscriber.onComplete();
                }
            };

            URI serverUri = new StandardConfiguration.Impl(xorcery.getInjectionManager().getInstance(Configuration.class)).getServerUri();
            reactiveStreams.publish(UriBuilder.fromUri(serverUri).scheme("ws").path("serversubscriber").build(), Configuration.empty(), clientPublisher, (Class<? extends Flow.Publisher<?>>) clientPublisher.getClass());

            clientPublisher.getDone().get();

            System.out.println("DONE!");
        }
    }

    public static class StringClientPublisher
            extends ClientPublisher<String> {
        @Override
        protected void publish(Flow.Subscriber<? super String> subscriber) {
            for (int i = 0; i < 100; i++) {
                subscriber.onNext(i + "");
            }
            subscriber.onComplete();
        }
    }

    public static class StringServerSubscriber
            extends ServerSubscriber<String> {
        @Override
        public void onNext(String item) {
            System.out.println(item);
            subscription.request(1);
        }
    }
}
