package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.server.Xorcery;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ClientPublisherServerSubscriberIT {

    private static final String config = """
            reactivestreams.enabled: true
            """;

    @SuppressWarnings("unchecked")
    @Test
    public void givenSubscriberWhenPublishEventsThenSubscriberConsumesEvents2() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config)).build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreams reactiveStreams = xorcery.getInjectionManager().getInstance(ReactiveStreams.class);

            // Server subscriber
            reactiveStreams.subscriber("/serversubscriber", cfg -> new StringServerSubscriber(), StringServerSubscriber.class);

            // Client publisher
            final long total = 1000;
            long start = System.currentTimeMillis();
            List<CompletionStage<Void>> futures = new ArrayList<>();
            int clients = 1;
            for (int i = 0; i < clients; i++) {
                final StringClientPublisher clientPublisher = new StringClientPublisher((int)total);
                StandardConfiguration standardConfiguration = ()->xorcery.getInjectionManager().getInstance(Configuration.class);
                URI serverUri = standardConfiguration.getServerUri();
                URI effectiveServerUri = adjustURIWithDynamicServerPort(xorcery, serverUri);
                futures.add(reactiveStreams.publish(UriBuilder.fromUri(effectiveServerUri).scheme(effectiveServerUri.getScheme().equals("https") ? "wss" : "ws").path("serversubscriber").build(), Configuration.empty(), clientPublisher, clientPublisher.getClass()));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            long end = System.currentTimeMillis();
            long time = end - start;
            System.out.printf("Time: %d, Rate:%d\n", time, (clients*total)/time);
        }
    }

    private static URI adjustURIWithDynamicServerPort(Xorcery xorcery, URI serverUri) throws URISyntaxException {
        String scheme = serverUri.getScheme();
        int port = -1;
        if ("https".equals(scheme)) {
            port = xorcery.getHttpsPort();
        } else if ("http".equals(scheme)) {
            port = xorcery.getHttpPort();
        }
        URI effectiveServerUri = new URI(scheme, serverUri.getUserInfo(), serverUri.getHost(), port, serverUri.getPath(), serverUri.getQuery(), serverUri.getFragment());
        return effectiveServerUri;
    }

    @Test
    public void givenSubscriberWhenPublishEventsWithResultsThenSubscriberCalculatesResults() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config)).build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
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

            StandardConfiguration standardConfiguration = ()->xorcery.getInjectionManager().getInstance(Configuration.class);
            URI serverUri = standardConfiguration.getServerUri();
            URI effectiveServerUri = adjustURIWithDynamicServerPort(xorcery, serverUri);
            reactiveStreams.publish(UriBuilder.fromUri(effectiveServerUri).scheme(effectiveServerUri.getScheme().equals("https") ? "wss" : "ws").path("serversubscriber").build(), Configuration.empty(), clientPublisher, (Class<? extends Flow.Publisher<?>>) clientPublisher.getClass())
                    .toCompletableFuture().get();

            System.out.println("DONE!");
        }
    }

    @Test
    public void givenSubscriberWhenPublishEventsWithResultsAndMetadataThenSubscriberCalculatesResults() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config)).build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
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
            final long total = 1000;
            final ClientPublisher<WithResult<WithMetadata<String>, Integer>> clientPublisher = new ClientPublisher<>() {
                @Override
                protected void publish(Flow.Subscriber<? super WithResult<WithMetadata<String>, Integer>> subscriber) {
                    for (int i = 0; i < total; i++) {
                        CompletableFuture<Integer> future = new CompletableFuture<>();
                        future.whenComplete((r, t)->
                                {
//                                    System.out.println("Length:"+r);
                                });
                        subscriber.onNext(new WithResult<>(new WithMetadata<>(new Metadata.Builder()
                                .add("foo", "bar")
                                .build(), i + "XORCERY"), future));
                    }
                    subscriber.onComplete();
                }
            };

            long start = System.currentTimeMillis();
            StandardConfiguration standardConfiguration = ()->xorcery.getInjectionManager().getInstance(Configuration.class);
            URI serverUri = standardConfiguration.getServerUri();
            URI effectiveServerUri = adjustURIWithDynamicServerPort(xorcery, serverUri);
            reactiveStreams.publish(UriBuilder.fromUri(effectiveServerUri).scheme(effectiveServerUri.getScheme().equals("https") ? "wss" : "ws").path("serversubscriber").build(), Configuration.empty(), clientPublisher, (Class<? extends Flow.Publisher<?>>) clientPublisher.getClass())
                            .whenComplete((r,t)->
                            {
                                System.out.println("Process complete");
                            }).toCompletableFuture().get();

            long end = System.currentTimeMillis();
            long time = end - start;
            System.out.printf("Time: %d, Rate:%d\n", time, total/time);
        }
    }

    public static class StringClientPublisher
            extends ClientPublisher<String> {

        private int total;

        public StringClientPublisher(int total) {
            this.total = total;
        }

        @Override
        protected void publish(Flow.Subscriber<? super String> subscriber) {
            for (int i = 0; i < total; i++) {
                subscriber.onNext(i + "");
            }
            subscriber.onComplete();
        }
    }

    public static class StringServerSubscriber
            extends ServerSubscriber<String> {
        @Override
        public void onNext(String item) {
//            System.out.println(item);
            subscription.request(1);
        }
    }
}
