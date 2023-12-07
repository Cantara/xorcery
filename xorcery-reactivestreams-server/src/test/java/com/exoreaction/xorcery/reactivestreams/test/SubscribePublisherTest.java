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

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import com.exoreaction.xorcery.net.Sockets;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.*;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreams.cancelStream;

public class SubscribePublisherTest {

    private String clientConf = """
                            instance.id: client
                            jetty.server.enabled: false
                            reactivestreams.server.enabled: false
            """;

    private String serverConf = """
                            instance.id: server
                            jetty.client.enabled: false
                            reactivestreams.client.enabled: false
                            jetty.server.http.enabled: false
                            jetty.server.ssl.port: "{{ SYSTEM.port }}"
            """;

    private Configuration clientConfiguration;
    private Configuration serverConfiguration;
    private ReactiveStreamsServerConfiguration reactiveStreamsServerConfiguration;

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        reactiveStreamsServerConfiguration = new ReactiveStreamsServerConfiguration(serverConfiguration.getConfiguration("reactivestreams.server"));
    }

    @Test
    public void testPublisherProducesResult() throws Exception {


        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(serverConfiguration);
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new ServerIntegerPublisher(100), ServerIntegerPublisher.class);

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(reactiveStreamsServerConfiguration.getURI(), "numbers",
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
    public void testSlowSubscriber() throws Exception {


        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(serverConfiguration);
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new ServerIntegerPublisher(10000), ServerIntegerPublisher.class);

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(reactiveStreamsServerConfiguration.getURI(), "numbers",
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
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).addYaml("""
                reactivestreams.server.idleTimeout: 3s
                """).build();
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new IntegerNoopPublisher(), IntegerNoopPublisher.class);

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(reactiveStreamsServerConfiguration.getURI(), "numbers",
                        Configuration::empty, subscriber, IntegerSubscriber.class, ClientConfiguration.defaults());

                Thread.sleep(5000);

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
    public void testSubscriberReconnect() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new IntegerNoopPublisher(), IntegerNoopPublisher.class);

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(reactiveStreamsServerConfiguration.getURI(), "numbers",
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
    public void testPublisherThrowsException() throws Exception {

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
                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(reactiveStreamsServerConfiguration.getURI(), "numbers",
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
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(reactiveStreamsServerConfiguration.getURI(), "numbers",
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
        CompletableFuture<Integer> result = new CompletableFuture<>();
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
                IntegerSubscriber subscriber = new IntegerSubscriber(result);
                stream = reactiveStreamsClient.subscribe(reactiveStreamsServerConfiguration.getURI(), "numbers",
                        Configuration::empty, subscriber, IntegerSubscriber.class, new ClientConfiguration.Builder().isRetryEnabled(false).build());

                Thread.sleep(2000);
            }
        }

        // Then
        Assertions.assertThrows(CompletionException.class, result::join);
        Assertions.assertThrows(CompletionException.class, stream::join);

    }

    private void report(Object result, Throwable throwable) {
        if (throwable != null)
            LogManager.getLogger().error("Error", throwable);
        else {
            LogManager.getLogger().info("Result:" + result);
        }
    }

    public static class IntegerSubscriber
            implements Subscriber<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        private int count = 0;
        private int total = 0;
        private Subscription subscription;
        private final CompletableFuture<Integer> result;

        public IntegerSubscriber(CompletableFuture<Integer> result) {
            this.result = result;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(100);
        }

        @Override
        public void onNext(Integer item) {
            total += item;
            count++;
            if (count % 100 == 0)
            {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                subscription.request(100);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            logger.info("Subscriber complete, total:" + total);
            result.complete(total);
        }
    }

    public static class ServerIntegerPublisher
            implements Publisher<Integer> {

        int max = 100;

        public ServerIntegerPublisher(int max) {
            this.max = max;
        }

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Subscription() {

                int current = 0;

                @Override
                public void request(long n) {

                    for (long i = 0; i < n && current < max; i++) {
                        subscriber.onNext(current++);
                    }

                    if (current == max)
                        subscriber.onComplete();
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    public static class IntegerNoopPublisher
            implements Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        private CompletableFuture<Subscriber<? super Integer>> subscriber = new CompletableFuture<>();

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            this.subscriber.complete(subscriber);
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {

                }

                @Override
                public void cancel() {
                    logger.info("Cancelled", new Exception());
                    subscriber.onComplete();
                }
            });
        }

        public CompletableFuture<Subscriber<? super Integer>> getSubscriber() {
            return subscriber;
        }
    }

    public static class ServerIntegerExceptionPublisher
            implements Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            subscriber.onError(new NotAuthorizedException("Not authorized"));
        }
    }

    public static class ClientConfigurationSubscriber
            implements Subscriber<Configuration> {

        Logger logger = LogManager.getLogger(getClass());
        private CompletableFuture<Configuration> future;
        private Configuration config;
        private Subscription subscription;


        public ClientConfigurationSubscriber(CompletableFuture<Configuration> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
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
            implements Publisher<Configuration> {

        Logger logger = LogManager.getLogger(getClass());
        private Configuration configuration;

        public ServerConfigurationPublisher(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void subscribe(Subscriber<? super Configuration> subscriber) {
            subscriber.onSubscribe(new Subscription() {

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
