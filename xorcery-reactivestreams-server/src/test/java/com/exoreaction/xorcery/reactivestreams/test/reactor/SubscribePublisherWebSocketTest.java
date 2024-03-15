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
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerTimeoutStreamException;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.reactor.WebSocketStreamsServerConfiguration;
import jakarta.ws.rs.core.HttpHeaders;
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
import reactor.core.Disposable;
import reactor.core.publisher.*;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.util.context.Context;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import reactor.util.retry.RetrySpec;

import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreams.onErrorDispose;

public class SubscribePublisherWebSocketTest {

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

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        websocketStreamsServerConfiguration = new WebSocketStreamsServerConfiguration(serverConfiguration.getConfiguration("reactivestreams.server"));
    }

    @Test
    public void testSubscribePublisher() throws Exception {
        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(serverConfiguration);
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                websocketStreamsServer.publisher(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        Flux.fromIterable(source));

                // When
                Flux<Integer> numbers = websocketStreamsClient.subscribe(
                        websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                        MediaType.APPLICATION_JSON, Integer.class, WebSocketClientOptions.empty());
                List<Integer> result = numbers.toStream().toList();

                // Then
                Assertions.assertEquals(source, result);
            } catch (Throwable throwable) {
                System.out.println(throwable);
            }
        }
    }

    @Test
    public void testSlowSubscriber() throws Exception {


        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(serverConfiguration);
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                List<Integer> source = IntStream.range(0, 10000).boxed().toList();
                Disposable publisherDisposable = websocketStreamsServer.publisher(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        Flux.fromIterable(source)
                );

                // When
                List<Integer> result = websocketStreamsClient.subscribe(
                                websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                MediaType.APPLICATION_JSON, Integer.class, WebSocketClientOptions.empty())
                        .map(v ->
                        {
                            if (v % 100 == 0) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            return v;
                        })
                        .toStream().toList();

                // Then
                Assertions.assertEquals(source, result);
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
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                Disposable publisherDisposable = websocketStreamsServer.publisher(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        new IntegerNoopPublisher()
                );

                // When
                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result);
                websocketStreamsClient.subscribe(
                                websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                MediaType.APPLICATION_JSON, Integer.class, WebSocketClientOptions.empty())
                        .subscribe(subscriber);

                Thread.sleep(5000);

                // Then
                Assertions.assertThrows(CompletionException.class, () ->
                {
                    result.orTimeout(5, TimeUnit.SECONDS)
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
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                Iterator<Publisher<Integer>> publisherIterator = List.<Publisher<Integer>>of(
                        TestPublisher.<Integer>createCold().next(1, 2, 3).error(new ServerStreamException("Blah")),
                        TestPublisher.<Integer>createCold().next(4, 5, 6).complete()
                ).iterator();

                websocketStreamsServer.publisher(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        subscriber -> publisherIterator.next().subscribe(subscriber)
                );

                // When
                Flux<Integer> numbers = websocketStreamsClient.subscribe(
                        websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                        MediaType.APPLICATION_JSON,
                        Integer.class, WebSocketClientOptions.empty());
                RetrySpec defaultRetrySpec = Retry.max(5);

                // Then
                List<Integer> result = numbers.retryWhen(defaultRetrySpec).toStream().toList();
                Assertions.assertEquals(List.of(1, 2, 3, 4, 5, 6), result);
            }
        }
    }

    @Test
    public void testPublisherThrowsException() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                TestPublisher<Integer> publisher = TestPublisher.<Integer>createCold().error(new NotAuthorizedStreamException("Not authorized"));
                Disposable publisherComplete = websocketStreamsServer.publisher(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        publisher);

                // When
                try {
                    int value = websocketStreamsClient.subscribe(
                                    websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                    MediaType.APPLICATION_JSON,
                                    Integer.class,
                                    WebSocketClientOptions.empty())
                            .take(1)
                            .blockFirst();

                    // Then
                    Assertions.fail();
                } catch (NotAuthorizedStreamException e) {
                    // Ok!
                }
            }
        }
    }

    @Test
    public void testSubscribeWithConfiguration()
            throws Exception {
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                Disposable publisherDisposable = websocketStreamsServer.publisher(
                        "configuration",
                        MediaType.APPLICATION_JSON,
                        Configuration.class,
                        new ServerConfigurationPublisher()
                );

                // When
                Configuration result = websocketStreamsClient.subscribe(
                                websocketStreamsServerConfiguration.getURI().resolve("configuration"),
                                MediaType.APPLICATION_JSON,
                                Configuration.class,
                                WebSocketClientOptions.empty())
                        .contextWrite(Context.of(HttpHeaders.AUTHORIZATION, "Bearer:abc"))
                        .take(1)
                        .blockFirst();

                // Then
                Assertions.assertEquals("Bearer:abc", result.getString("Authorization").orElseThrow());
            }
        }
    }

    @Test
    public void testPublisherUnavailableWithoutRetryCausesOnError() throws Exception {

        // Given
        CompletableFuture<Integer> result = new CompletableFuture<>();

        try (Xorcery client = new Xorcery(clientConfiguration)) {
            try (Xorcery server = new Xorcery(serverConfiguration)) {
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                // When
                IntegerSubscriber subscriber = new IntegerSubscriber(result);
                websocketStreamsClient.subscribe(
                                websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                MediaType.APPLICATION_JSON,
                                Integer.class,
                                WebSocketClientOptions.empty())
                        .subscribe(subscriber);

                Thread.sleep(2000);
            }
        }

        // Then
        Assertions.assertThrows(CompletionException.class, result::join);
    }

    private void report(Object result, Throwable throwable) {
        if (throwable != null)
            LogManager.getLogger().error("Error", throwable);
        else {
            LogManager.getLogger().info("Result:" + result);
        }
    }

    public static class IntegerSubscriber
            extends BaseSubscriber<Integer> {

        private int count = 0;
        private int total = 0;
        private final CompletableFuture<Integer> result;

        public IntegerSubscriber(CompletableFuture<Integer> result) {
            this.result = result;
        }

        @Override
        protected void hookOnNext(Integer item) {
            total += item;
            count++;
            if (count % 100 == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                request(100);
            }
        }

        @Override
        protected void hookOnComplete() {
            result.complete(total);
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            result.completeExceptionally(throwable);
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
            subscriber.onError(new NotAuthorizedStreamException("Not authorized"));
        }
    }

    public static class ServerConfigurationPublisher
            implements Publisher<Configuration> {

        Logger logger = LogManager.getLogger(getClass());

        public ServerConfigurationPublisher() {
        }

        @Override
        public void subscribe(Subscriber<? super Configuration> subscriber) {

            Configuration.Builder builder = new Configuration.Builder();
            if (subscriber instanceof CoreSubscriber<? super Configuration> coreSubscriber) {
                coreSubscriber.currentContext().forEach((k, v) -> builder.add(k.toString(), v.toString()));
                Configuration configuration = builder.build();

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
            } else subscriber.onError(new IllegalArgumentException("Subscriber must be CoreSubscriber"));
        }
    }
}
