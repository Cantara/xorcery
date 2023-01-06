package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.exoreaction.xorcery.util.Sockets;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static com.exoreaction.xorcery.service.reactivestreams.util.ReactiveStreams.cancelStream;

public class ReactiveStreamsLifecycleTest {

    Logger logger = LogManager.getLogger(getClass());

    @Test
    public void testSubscriberProducesResult() throws Exception {

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

            CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new ServerIntegerPublisher(), ServerIntegerPublisher.class);

            // When
            CompletableFuture<Integer> result = new CompletableFuture<>();
            ClientIntegerSubscriber subscriber = new ClientIntegerSubscriber(result);
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(standardConfiguration.getServerUri().getAuthority(), "numbers",
                    Configuration::empty, subscriber, ClientIntegerSubscriber.class, Configuration.empty());

            // Then
            result.whenComplete(this::report).toCompletableFuture().join();
        }
    }

    @Test
    public void testSubscriberTimesOut() throws Exception {

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
    public void testSubscriberServerException() throws Exception {

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
                    result.orTimeout(5, TimeUnit.SECONDS)
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
    public void testSubscribeWithConfiguration()
            throws Exception
    {
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

            CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config ->
                    new ServerConfigurationPublisher(config), ServerConfigurationPublisher.class);

            // When
            CompletableFuture<Configuration> result = new CompletableFuture<>();
            ClientConfigurationSubscriber subscriber = new ClientConfigurationSubscriber(result);
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(standardConfiguration.getServerUri().getAuthority(), "numbers",
                    ()->new Configuration.Builder().add(HttpHeaders.AUTHORIZATION, "Bearer:abc").build(), subscriber, ClientConfigurationSubscriber.class, Configuration.empty());

            // Then
                try {
                    result.orTimeout(5, TimeUnit.SECONDS)
                            .exceptionallyCompose(cancelStream(stream))
                            .whenComplete((cfg, throwable)->
                            {
                                Assertions.assertEquals("Bearer:abc", cfg.getString("Authorization").orElseThrow());
                            })
                            .toCompletableFuture().join();
                } catch (Exception e) {
                    throw e;
                }
        }

    }

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

    public static class ServerIntegerPublisher
            implements Flow.Publisher<Integer> {

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {

                int current = 0;
                int max = 100;

                @Override
                public void request(long n) {

                    for (long i = 0; i < n && current < max; i++) {
                        subscriber.onNext(current++);
                    }

                    if (current == 100)
                        subscriber.onComplete();
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

    public static class ClientConfigurationSubscriber
            implements Flow.Subscriber<Configuration> {

        private CompletableFuture<Configuration> future;


        public ClientConfigurationSubscriber(CompletableFuture<Configuration> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(1);
        }

        @Override
        public void onNext(Configuration item) {
            future.complete(item);
        }

        @Override
        public void onError(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
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

                    for (long i = 0; i < n ; i++) {
                        subscriber.onNext(configuration);
                    }

                    subscriber.onComplete();
                }

                @Override
                public void cancel() {
                }
            });
        }
    }
}
