/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.reactivestreams.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.util.Sockets;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreams.cancelStream;

public class ReactiveStreamsLifecycleTest {

    private InstanceConfiguration serverInstanceConfiguration;
    private Configuration clientConfiguration;
    private Configuration serverConfiguration;

    @BeforeEach
    public void setup() {
        clientConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("instance.id", "client")
                .add("jetty.server.enabled", false)
                .add("reactivestreams.server.enabled", false)
                .build();
        serverConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("instance.id", "server")
                .add("jetty.client.enabled", false)
                .add("reactivestreams.client.enabled", false)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();

        serverInstanceConfiguration = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
    }

    @Test
    public void testSubscriberProducesResult() throws Exception {


        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(serverConfiguration);
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new ServerIntegerPublisher(), ServerIntegerPublisher.class);

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result, new CompletableFuture<>());
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, subscriber, IntegerSubscriber.class, ClientConfiguration.defaults());

                // Then
                result.orTimeout(10, TimeUnit.SECONDS)
                        .exceptionallyCompose(cancelStream(stream))
                        .whenComplete(this::report)
                        .toCompletableFuture().join();
            }
        }
    }

    @Test
    public void testSubscriberTimesOut() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new IntegerNoopPublisher(), IntegerNoopPublisher.class);

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result, new CompletableFuture<>());
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, subscriber, IntegerSubscriber.class, ClientConfiguration.defaults());

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
    }

    @Test
    public void testSubscriberServerException() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config ->
                {
                    return new ServerIntegerExceptionPublisher();
                }, ServerIntegerExceptionPublisher.class);

                // When
                CompletableFuture<Void> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(new CompletableFuture<>(), result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, subscriber, IntegerSubscriber.class, ClientConfiguration.defaults());

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
    }

    @Test
    public void testSubscribeWithConfiguration()
            throws Exception {
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config ->
                        new ServerConfigurationPublisher(config), ServerConfigurationPublisher.class);

                // When
                CompletableFuture<Configuration> result = new CompletableFuture<>();
                ClientConfigurationSubscriber subscriber = new ClientConfigurationSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        () -> new Configuration.Builder().add(HttpHeaders.AUTHORIZATION, "Bearer:abc").build(), subscriber, ClientConfigurationSubscriber.class, ClientConfiguration.defaults());

                // Then
                try {
                    result.orTimeout(10, TimeUnit.SECONDS)
                            .exceptionallyCompose(cancelStream(stream))
                            .whenComplete((cfg, throwable) ->
                            {
                                LogManager.getLogger().info("Configuration/throwable:" + cfg, throwable);
                                Assertions.assertEquals("Bearer:abc", cfg.getString("Authorization").orElseThrow());
                            })
                            .toCompletableFuture().join();
                } catch (Exception e) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testPublisherShutdownWithoutRetryCausesOnError() throws Exception {

        // Given
        CompletableFuture<Void> error = new CompletableFuture<>();
        CompletableFuture<Void> stream = null;

        try (Xorcery client = new Xorcery(clientConfiguration)) {
            try (Xorcery server = new Xorcery(serverConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config ->
                {
                    return new IntegerNoopPublisher();
                }, IntegerNoopPublisher.class);

                // When
                IntegerSubscriber subscriber = new IntegerSubscriber(new CompletableFuture<>(), error);
                stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, subscriber, IntegerSubscriber.class, new ClientConfiguration.Builder().isRetryEnabled(false).build());

                Thread.sleep(2000);
            }
        }

        // Then
        Assertions.assertThrows(CompletionException.class, error::join);
        Assertions.assertThrows(CompletionException.class, stream::join);

    }

    @Test
    public void testInactivePublisherCausesReconnect() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration.asBuilder()
                .add("reactivestreams.server.idleTimeout", "3s")
                .build())) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Integer> subscriberTotal = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(subscriberTotal, new CompletableFuture<>());
                CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> subscriber, IntegerSubscriber.class);

                // When
                IntegerNoopPublisher publisher = new IntegerNoopPublisher();
                CompletableFuture<Void> stream = reactiveStreamsClient.publish(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, publisher, IntegerNoopPublisher.class, ClientConfiguration.defaults());

                // Wait
                Thread.sleep(5000);
                publisher.getSubscriber().join().onNext(1);
                Thread.sleep(5000);
                publisher.getSubscriber().join().onNext(1);
                Thread.sleep(5000);
                publisher.getSubscriber().join().onNext(1);
                publisher.getSubscriber().join().onComplete();
                LogManager.getLogger().info("Completed publisher");

                // Then
                Assertions.assertEquals(3, subscriberTotal.join());
            }
        }
    }

    private void report(Object result, Throwable throwable) {
        if (throwable != null)
            LogManager.getLogger().error("Error", throwable);
        else {
            LogManager.getLogger().info("Result:" + result);
        }
    }

    public static class IntegerSubscriber
            implements Flow.Subscriber<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        private int total = 0;
        private Flow.Subscription subscription;
        private final CompletableFuture<Integer> complete;
        private final CompletableFuture<Void> error;

        public IntegerSubscriber(CompletableFuture<Integer> complete, CompletableFuture<Void> error) {
            this.complete = complete;
            this.error = error;
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
            error.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            logger.info("Subscriber complete, total:" + total);
            complete.complete(total);
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

    public static class IntegerNoopPublisher
            implements Flow.Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        private CompletableFuture<Flow.Subscriber<? super Integer>> subscriber = new CompletableFuture<>();

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            this.subscriber.complete(subscriber);
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

        public CompletableFuture<Flow.Subscriber<? super Integer>> getSubscriber() {
            return subscriber;
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

        Logger logger = LogManager.getLogger(getClass());
        private CompletableFuture<Configuration> future;
        private Configuration config;
        private Flow.Subscription subscription;


        public ClientConfigurationSubscriber(CompletableFuture<Configuration> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(Configuration item) {
            logger.info("Received configuration:" + item);
            config = item;
            subscription.cancel();
        }

        @Override
        public void onError(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            logger.info("Subscriber complete:" + config);
            future.complete(config);
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
