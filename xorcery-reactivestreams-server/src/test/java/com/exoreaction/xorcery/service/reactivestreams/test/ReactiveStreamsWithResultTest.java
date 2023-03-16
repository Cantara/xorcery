package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.util.Sockets;
import jakarta.ws.rs.NotAuthorizedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.exoreaction.xorcery.service.reactivestreams.util.ReactiveStreams.cancelStream;

public class ReactiveStreamsWithResultTest {

    Logger logger = LogManager.getLogger(getClass());

    @Test
    public void testServerProducesResult() throws Exception {

        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        InstanceConfiguration standardConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> new ServerIntegerSubscriber(), ServerIntegerSubscriber.class);

            // When
            AtomicInteger result = new AtomicInteger(0);
            ClientIntegerPublisher subscriber = new ClientIntegerPublisher(result);
            CompletableFuture<Void> stream = reactiveStreamsClient.publish(standardConfiguration.getURI().getAuthority(), "numbers",
                    Configuration::empty, subscriber, ClientIntegerPublisher.class, Configuration.empty());

            // Then
            stream.orTimeout(1000, TimeUnit.SECONDS)
                    .exceptionallyCompose(cancelStream(stream))
                    .whenComplete((r, t) ->
                    {
                        report(result.get(), t);
                    })
                    .toCompletableFuture().join();
        }
    }
/*
    @Test
    public void testServerTimesOut() throws Exception {

        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new ServerIntegerNoopPublisher(), ServerIntegerNoopPublisher.class);

            // When
            CompletableFuture<Integer> result = new CompletableFuture<>();
            ClientIntegerSubscriber subscriber = new ClientIntegerSubscriber(result);
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(standardConfiguration.getServerUri().getAuthority(), "numbers",
                    Configuration::empty, subscriber, ClientIntegerSubscriber.class, Configuration.empty());

            // Then
            Assertions.assertThrows(CompletionException.class, () ->
            {
                result.orTimeout(5, TimeUnit.SECONDS)
                        .exceptionallyCompose(cancelStream(stream))
                        .whenComplete(this::report)
                        .toCompletableFuture().join();
            });
        }
    }

    @Test
    public void testServerException() throws Exception {

        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config ->
            {
                return new ServerIntegerExceptionPublisher();
            }, ServerIntegerExceptionPublisher.class);

            // When
            CompletableFuture<Integer> result = new CompletableFuture<>();
            ClientIntegerSubscriber subscriber = new ClientIntegerSubscriber(result);
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(standardConfiguration.getServerUri().getAuthority(), "numbers",
                    Configuration::empty, subscriber, ClientIntegerSubscriber.class, Configuration.empty());

            // Then
            Assertions.assertThrows(NotAuthorizedException.class, () ->
            {
                try {
                    result.orTimeout(10, TimeUnit.SECONDS)
                            .exceptionallyCompose(cancelStream(stream))
                            .whenComplete(this::report)
                            .toCompletableFuture().join();
                } catch (Throwable e) {
                    throw e.getCause();
                }
            });
        }
    }

    @Test
    public void testServerWithConfiguration()
            throws Exception {
        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config ->
                    new ServerConfigurationPublisher(config), ServerConfigurationPublisher.class);

            // When
            CompletableFuture<Configuration> result = new CompletableFuture<>();
            ClientConfigurationSubscriber subscriber = new ClientConfigurationSubscriber(result);
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(standardConfiguration.getServerUri().getAuthority(), "numbers",
                    () -> new Configuration.Builder().add(HttpHeaders.AUTHORIZATION, "Bearer:abc").build(), subscriber, ClientConfigurationSubscriber.class, Configuration.empty());

            // Then
            try {
                result.orTimeout(10, TimeUnit.SECONDS)
                        .exceptionallyCompose(cancelStream(stream))
                        .whenComplete((cfg, throwable) ->
                        {
                            logger.info("Configuration/throwable:" + cfg, throwable);
                            Assertions.assertEquals("Bearer:abc", cfg.getString("Authorization").orElseThrow());
                        })
                        .toCompletableFuture().join();
            } catch (Exception e) {
                throw e;
            }
        }

    }*/

    private void report(Integer total, Throwable throwable) {
        if (throwable != null)
            logger.error("Error", throwable);
        else {
            logger.info("Total:" + total);
            Assertions.assertEquals(4950, total);
        }
    }

    public static class ClientIntegerSubscriber
            implements Flow.Subscriber<Integer> {

        private int total = 0;
        private Flow.Subscription subscription;
        private CompletableFuture<Integer> future;


        public ClientIntegerSubscriber(CompletableFuture<Integer> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(Integer item) {
            total += item;
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            future.complete(total);
        }
    }

    public static class ClientIntegerPublisher
            implements Flow.Publisher<WithResult<Integer, Integer>> {

        private final Logger logger = LogManager.getLogger(getClass());
        private final AtomicInteger result;

        public ClientIntegerPublisher(AtomicInteger result) {
            this.result = result;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super WithResult<Integer, Integer>> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {

                int current = 0;
                int max = 100;

                @Override
                public void request(long n) {

                    for (long i = 0; i < n && current < max; i++) {

                        CompletableFuture<Integer> response = new CompletableFuture<Integer>();
                        response.whenComplete((v, t)->
                        {
                            logger.info("Result:" + v);
                            result.addAndGet(v);
                        });
                        subscriber.onNext(new WithResult<>(current++, response));
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

    public static class ServerIntegerNoopPublisher
            implements Flow.Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {

                }

                @Override
                public void cancel() {
                    logger.info("Cancelled");
                }
            });
        }
    }

    public static class ServerIntegerExceptionPublisher
            implements Flow.Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            subscriber.onError(new NotAuthorizedException("Not authorized"));
        }
    }

    public static class ServerIntegerSubscriber
            implements Flow.Subscriber<WithResult<Integer, Integer>> {

        Logger logger = LogManager.getLogger(getClass());
        private Configuration config;
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(100);
        }

        @Override
        public void onNext(WithResult<Integer, Integer> item) {
            CompletableFuture.delayedExecutor(new Random().nextInt(5), TimeUnit.SECONDS).execute(()->
                    item.result().complete(item.event()));
            logger.info("Received integer:" + item.event());
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

    public static class ServerConfigurationPublisher
            implements Flow.Publisher<Configuration> {

        Logger logger = LogManager.getLogger(getClass());
        private Configuration configuration;

        public ServerConfigurationPublisher(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Configuration> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {

                @Override
                public void request(long n) {

                    logger.info("Sending configuration to subscriber");
                    for (long i = 0; i < n; i++) {
                        subscriber.onNext(configuration);
                    }
                }

                @Override
                public void cancel() {
                    logger.info("Complete");
                    subscriber.onComplete();
                }
            });
        }
    }
}
