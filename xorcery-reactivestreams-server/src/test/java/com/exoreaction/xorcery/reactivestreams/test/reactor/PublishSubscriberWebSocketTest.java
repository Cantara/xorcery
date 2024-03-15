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
package com.exoreaction.xorcery.reactivestreams.test.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import com.exoreaction.xorcery.reactivestreams.server.reactor.WebSocketStreamsServerConfiguration;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreams.cancelStream;

public class PublishSubscriberWebSocketTest {

    private String clientConf = """
                            instance.id: client
                            jetty.server.enabled: false
                            reactivestreams.server.enabled: false
                            reactivestreams.reactor.server.enabled: false
            """;

    private String serverConf = """
                            instance.id: server
                            jetty.client.enabled: false
                            reactivestreams.client.enabled: false
                            reactivestreams.reactor.client.enabled: false
                            jetty.server.http.enabled: false
                            jetty.server.ssl.port: "{{ SYSTEM.port }}"
            """;

    private Configuration clientConfiguration;
    private Configuration serverConfiguration;
    private WebSocketStreamsServerConfiguration websocketStreamsServerConfiguration;

    Logger logger = LogManager.getLogger();

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        websocketStreamsServerConfiguration = new WebSocketStreamsServerConfiguration(serverConfiguration.getConfiguration("reactivestreams.server"));
    }

    @Test
    public void testPublishSubscriber() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                List<Integer> result = new ArrayList<>();
                websocketStreamsServer.subscriber(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        upstream -> upstream.doOnNext(result::add));

                // When
                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                websocketStreamsClient.publish(
                                websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                MediaType.APPLICATION_JSON,
                                Integer.class,
                                WebSocketClientOptions.empty(),
                                Flux.fromIterable(source))
                        .blockLast();

                // Then
                Assertions.assertEquals(source, result);
            }
        }
    }

    @Test
    public void testServerTimesOut() throws Exception {

        // Given
        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(String.format("""
                reactivestreams.server.idleTimeout: "5s"
                jetty.server.http.port: %d
                jetty.server.ssl.port: %d
                """, Sockets.nextFreePort(), Sockets.nextFreePort())).build();
        logger.debug(configuration);

        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> publisherComplete = reactiveStreamsServer.publisher("numbers", config -> new ServerIntegerNoopPublisher(), ServerIntegerNoopPublisher.class);

            // When
            CompletableFuture<Integer> result = new CompletableFuture<>();
            ClientIntegerSubscriber subscriber = new ClientIntegerSubscriber(result);
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(ReactiveStreamsServerConfiguration.get(configuration).getURI(), "numbers",
                    Configuration::empty, subscriber, ClientIntegerSubscriber.class, ClientConfiguration.defaults());

            // Then
            Assertions.assertThrows(CompletionException.class, () ->
            {
                result.orTimeout(20, TimeUnit.SECONDS)
                        .exceptionallyCompose(cancelStream(stream))
                        .whenComplete(this::report)
                        .toCompletableFuture().join();
            });
        }
    }

    @Test
    public void testServerException() throws Exception {

        // Given
        Configuration configuration = new ConfigurationBuilder()
                .addTestDefaults()
                .addYaml(String.format("""
                        jetty.server.http.port: %d
                        jetty.server.ssl.port: %d
                        """, Sockets.nextFreePort(), Sockets.nextFreePort())).build();
        logger.debug(configuration);

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
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe(ReactiveStreamsServerConfiguration.get(configuration).getURI(), "numbers",
                    Configuration::empty, subscriber, ClientIntegerSubscriber.class, ClientConfiguration.defaults());

            // Then
            Assertions.assertThrows(NotAuthorizedStreamException.class, () ->
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
    public void testServerWithContext()
            throws Exception {
        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                websocketStreamsServer.subscriberWithResult(
                        "uri-template|/numbers/{foo}",
                        MediaType.APPLICATION_JSON,
                        String.class,
                        String.class,
                        upstream -> upstream.contextWrite(Context.of("server", "123")));

                // When
                Publisher<String> configPublisher = s -> {
                    if (s instanceof CoreSubscriber<? super String> subscriber) {
                        String val = subscriber.currentContext().stream().toList().toString();
                        s.onSubscribe(new Subscription() {
                            @Override
                            public void request(long n) {
                                subscriber.onNext(val);
                            }

                            @Override
                            public void cancel() {
                                subscriber.onComplete();
                            }
                        });
                    }
                };

                String config = websocketStreamsClient.publishWithResult(
                                websocketStreamsServerConfiguration.getURI().resolve("numbers/bar?param1=value1"),
                                MediaType.APPLICATION_JSON,
                                String.class,
                                String.class,
                                WebSocketClientOptions.empty(),
                                configPublisher)
                        .contextWrite(Context.of("client", "abc"))
                        .take(1).blockFirst();

                // Then
                Assertions.assertEquals("[server=123, foo=bar, client=abc, param1=value1]", config);
            }
        }
    }

    private void report(Integer total, Throwable throwable) {
        Logger logger = LogManager.getLogger(getClass());

        if (throwable != null)
            logger.error("Error", throwable);
        else {
            logger.info("Total:" + total);
            Assertions.assertEquals(4950, total);
        }
    }

    public static class ClientIntegerSubscriber
            implements Subscriber<Integer> {

        private int total = 0;
        private Subscription subscription;
        private CompletableFuture<Integer> future;


        public ClientIntegerSubscriber(CompletableFuture<Integer> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
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

    public static class ClientIntegerWithResultPublisher
            extends ClientWithResultPublisher<Integer, Integer>
            implements Publisher<WithResult<Integer, Integer>> {
    }

    public static class ClientWithResultPublisher<T, R>
            implements AutoCloseable {

        private Subscriber<? super WithResult<T, R>> subscriber;
        private final Semaphore requested = new Semaphore(0);

        public ClientWithResultPublisher() {
        }

        public void subscribe(Subscriber<? super WithResult<T, R>> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Subscription() {

                @Override
                public void request(long n) {

                    requested.release((int) n);
                }

                @Override
                public void cancel() {
                }
            });
        }

        public CompletableFuture<R> publish(T item) {
            CompletableFuture<R> response = new CompletableFuture<R>();
            try {
                requested.acquire();
                subscriber.onNext(new WithResult<>(item, response));
                return response;
            } catch (InterruptedException e) {
                response.completeExceptionally(e);
                return response;
            }
        }

        @Override
        public void close() throws Exception {
            subscriber.onComplete();
        }
    }

    public static class ServerIntegerNoopPublisher
            implements Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
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
            implements Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            subscriber.onError(new NotAuthorizedStreamException("Not authorized"));
        }
    }

    public static class ServerIntegerSubscriber
            implements Subscriber<WithResult<Integer, Integer>> {

        Logger logger = LogManager.getLogger(getClass());
        private Configuration config;
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(100);
        }

        @Override
        public void onNext(WithResult<Integer, Integer> item) {
            item.result().complete(item.event());
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
