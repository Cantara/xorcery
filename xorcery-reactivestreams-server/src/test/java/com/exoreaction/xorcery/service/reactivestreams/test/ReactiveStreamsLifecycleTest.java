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
package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.reactivestreams.api.ClientConfiguration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.exoreaction.xorcery.service.reactivestreams.api.ServerShutdownStreamException;
import com.exoreaction.xorcery.util.Sockets;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static com.exoreaction.xorcery.service.reactivestreams.util.ReactiveStreams.cancelStream;

public class ReactiveStreamsLifecycleTest {

    Logger logger = LogManager.getLogger(getClass());
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
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new ServerIntegerPublisher(), ServerIntegerPublisher.class);

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                ClientIntegerSubscriber subscriber = new ClientIntegerSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, subscriber, ClientIntegerSubscriber.class, ClientConfiguration.defaults());

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

                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new ServerIntegerNoopPublisher(), ServerIntegerNoopPublisher.class);

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                ClientIntegerSubscriber subscriber = new ClientIntegerSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, subscriber, ClientIntegerSubscriber.class, ClientConfiguration.defaults());

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
                CompletableFuture<Integer> result = new CompletableFuture<>();
                ClientIntegerSubscriber subscriber = new ClientIntegerSubscriber(result);
                CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, subscriber, ClientIntegerSubscriber.class, ClientConfiguration.defaults());

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
                                logger.info("Configuration/throwable:" + cfg, throwable);
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
                    return new ServerIntegerNoopPublisher();
                }, ServerIntegerNoopPublisher.class);

                // When
                ClientIntegerSubscriber subscriber = new ClientIntegerSubscriber(result);
                stream = reactiveStreamsClient.subscribe(serverInstanceConfiguration.getURI().getAuthority(), "numbers",
                        Configuration::empty, subscriber, ClientIntegerSubscriber.class, new ClientConfiguration.Builder().isRetryEnabled(false).build());

                Thread.sleep(2000);
            }
        }

        // Then
        Assertions.assertThrows(CompletionException.class, result::join);
        Assertions.assertThrows(CompletionException.class, stream::join);

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
