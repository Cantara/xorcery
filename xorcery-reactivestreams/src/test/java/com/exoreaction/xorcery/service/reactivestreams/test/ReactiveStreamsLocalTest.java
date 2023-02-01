package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.exoreaction.xorcery.util.Sockets;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.exoreaction.xorcery.service.reactivestreams.util.ReactiveStreams.cancelStream;

public class ReactiveStreamsLocalTest {

    @Test
    public void testLocalPublishWithTypeConversion() throws Exception {
        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("server.http.port", Sockets.nextFreePort())
                .add("server.ssl.port", Sockets.nextFreePort())
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> new JsonSubscriber(), JsonSubscriber.class);

            // When
            IntegerPublisher subscriber = new IntegerPublisher();
            CompletableFuture<Void> stream = reactiveStreamsClient.publish(null, "numbers",
                    Configuration::empty, subscriber, IntegerPublisher.class, Configuration.empty());

            // Then
            stream.orTimeout(1000, TimeUnit.SECONDS)
                    .exceptionallyCompose(cancelStream(stream))
                    .toCompletableFuture().join();
        }
    }

    @Test
    public void testLocalSubscribeWithTypeConversion() throws Exception {
        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("server.http.port", Sockets.nextFreePort())
                .add("server.ssl.port", Sockets.nextFreePort())
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.publisher("numbers", config -> new IntegerPublisher(), IntegerPublisher.class);

            // When
            JsonSubscriber subscriber = new JsonSubscriber();
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(null, "numbers",
                    Configuration::empty, subscriber, JsonSubscriber.class, Configuration.empty());

            // Then
            stream.orTimeout(1000, TimeUnit.SECONDS)
                    .exceptionallyCompose(cancelStream(stream))
                    .toCompletableFuture().join();
        }
    }

    public static class IntegerPublisher
            implements Flow.Publisher<Integer> {

        private final Logger logger = LogManager.getLogger(getClass());

        public IntegerPublisher() {
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {

                int current = 0;
                int max = 100;

                @Override
                public void request(long n) {

                    for (long i = 0; i < n && current < max; i++) {

                        CompletableFuture<Integer> response = new CompletableFuture<Integer>();
                        response.whenComplete((v, t) ->
                        {
                            logger.info("Result:" + v);
                        });
                        subscriber.onNext(current++);
                    }

                    if (current == 100) {
                        logger.info("Subscriber complete");
                        subscriber.onComplete();
                        current++;
                    }
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    public static class JsonSubscriber
            implements Flow.Subscriber<JsonNode> {

        Logger logger = LogManager.getLogger(getClass());
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(100);
        }

        @Override
        public void onNext(JsonNode item) {
            logger.info("Received integer:" + item);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error("Subscriber error", throwable);
        }

        @Override
        public void onComplete() {
            logger.info("Subscriber onComplete");
        }
    }
}
