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
package dev.xorcery.reactivestreams.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.net.Sockets;
import dev.xorcery.reactivestreams.api.IdleTimeoutStreamException;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.api.server.ServerStreamException;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import dev.xorcery.reactivestreams.server.ServerWebSocketStreamsConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions.instance;

@Timeout(60)
public class PublishWithResultSubscriberWebSocketTest {

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
    private ServerWebSocketStreamsConfiguration websocketStreamsServerWebSocketStreamsConfiguration;

    Logger logger = LogManager.getLogger();

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        websocketStreamsServerWebSocketStreamsConfiguration = ServerWebSocketStreamsConfiguration.get(serverConfiguration);
    }

    @Test
    public void complete() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.map(v -> v));

                // When
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                List<Integer> result = websocketStreamsClientClient.publishWithResult(Flux.fromIterable(source), serverUri, instance(), Integer.class, Integer.class)
                        .toStream().toList();

                // Then
                Assertions.assertEquals(source, result);
            }
        }
    }

    @Test
    public void cancel() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                CountDownLatch cancelled = new CountDownLatch(1);
                CountDownLatch latch = new CountDownLatch(1);
                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Integer.class,
                        upstream -> upstream
                                .doOnCancel(cancelled::countDown));

                // When
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                Sinks.Many<Integer> publisher = Sinks.many().unicast().onBackpressureBuffer(new ArrayBlockingQueue<>(1024));
                List<Integer> result = new ArrayList<>();
                Disposable subscription = websocketStreamsClientClient.publishWithResult(publisher.asFlux(), serverUri, instance(), Integer.class, Integer.class)
                        .doOnError(e -> logger.error("ERROR", e))
                        .doOnNext(result::add)
                        .doOnNext(v -> latch.countDown())
                        .subscribe();

                logger.info("Emitted: " + publisher.tryEmitNext(42));
                latch.await(10, TimeUnit.SECONDS);
                subscription.dispose();
                logger.info("Disposed");
                cancelled.await(10, TimeUnit.SECONDS);

                // Then
                Assertions.assertEquals(List.of(42), result);
            }
        }
    }

    @Test
    public void subscriberException() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.handle((v, s) ->
                        {
                            if (v == 10)
                                s.error(new IllegalArgumentException("Break"));
                            else
                                s.next(v);
                        }));

                // Then
                try {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    List<Integer> source = IntStream.range(0, 100).boxed().toList();
                    websocketStreamsClientClient.publishWithResult(Flux.fromIterable(source), serverUri, instance(), Integer.class, Integer.class)
                            .toStream().toList();
                    Assertions.fail();
                } catch (ServerStreamException e) {
                    Assertions.assertEquals(500, e.getStatus());
                    Assertions.assertEquals("Break", e.getMessage());
                }
            }
        }
    }

    @Test
    public void serverIdleTimeout() throws Exception {

        // Given
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf)
                .addYaml("""
                        jetty.server.websockets.idleTimeout: "2s"
                        """)
                .build();
        logger.info(serverConfiguration);

        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.doOnNext(System.out::println));

                // When

                // Then
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();
                    websocketStreamsClientClient.publishWithResult(sink.asFlux().doOnNext(System.out::println), serverUri, instance(), Integer.class, Integer.class)
                            .toStream().toList();
                });
            }
        }
    }

    @Test
    public void clientIdleTimeout() throws Exception {

        // Given
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf)
                .addYaml("""
                        reactivestreams.client.idleTimeout: "2s"
                        """)
                .build();
        logger.info(serverConfiguration);

        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.doOnNext(System.out::println));

                // When

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();
                    websocketStreamsClientClient.publishWithResult(sink.asFlux(), serverUri, instance(), Integer.class, Integer.class)
                            .toStream().toList();
                });
            }
        }
    }

    @Test
    public void clientIdleTimeoutWithRetry() throws Exception {

        // Given
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf)
                .addYaml("""
                        reactivestreams.client.idleTimeout: "1s"
                        """)
                .build();
        logger.info(serverConfiguration);

        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                List<Integer> result = new ArrayList<>();
                CountDownLatch latch = new CountDownLatch(1);
                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Integer.class,
                        upstream -> upstream);

                // When
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                Sinks.Many<Integer> sink = Sinks.many().replay().all();

                websocketStreamsClientClient.publishWithResult(sink.asFlux(), serverUri, instance(), Integer.class, Integer.class)
                        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3))
                                .doBeforeRetry(rs ->
                                {
                                    result.clear();
                                    logger.warn(rs.failure());
                                }))
                        .doOnNext(result::add)
                        .doOnComplete(latch::countDown)
                        .subscribe();

                IntStream.range(0, 5).boxed().forEach(v ->
                {
                    try {
                        sink.tryEmitNext(v);
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                sink.tryEmitComplete();

                latch.await(20, TimeUnit.SECONDS);

                // Then
                Assertions.assertEquals(List.of(0, 1, 2, 3, 4), result);
            }
        }
    }

    @Test
    public void withContext()
            throws Exception {
        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers/{foo}",
                        ServerWebSocketOptions.instance(),
                        String.class,
                        String.class,
                        upstream -> upstream.contextWrite(Context.of("server", "123")));

                // When
                Publisher<String> configPublisher = s -> {
                    if (s instanceof CoreSubscriber<? super String> subscriber) {
                        String val = subscriber.currentContext().stream().filter(e -> !(e.getKey().equals("request") || e.getKey().equals("response"))).toList().toString();
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

                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers/bar?param1=value1");
                String config = websocketStreamsClientClient.publishWithResult(Flux.from(configPublisher), serverUri, instance(), String.class, String.class)
                        .contextWrite(Context.of("client", "abc"))
                        .take(1).blockFirst();

                // Then
                Assertions.assertEquals("[server=123, foo=bar, client=abc, param1=value1]", config);
            }
        }
    }

    @Test
    public void completeBatching() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.map(v -> v));

                // When
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                List<Integer> result = websocketStreamsClientClient.publishWithResult(Flux.fromIterable(source), serverUri, ClientWebSocketOptions.instance(), Integer.class, Integer.class)
                        .toStream().toList();

                // Then
                Assertions.assertEquals(source, result);
            }
        }
    }

}
