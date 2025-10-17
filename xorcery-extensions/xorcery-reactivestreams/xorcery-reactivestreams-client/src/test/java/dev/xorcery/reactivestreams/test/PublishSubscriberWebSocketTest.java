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
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import dev.xorcery.reactivestreams.server.ServerWebSocketStreamsConfiguration;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@Timeout(60)
public class PublishSubscriberWebSocketTest {

    private String clientConf = """
                            instance.id: client
                            jetty.server.enabled: false
                            reactivestreams.server.enabled: false
                            jetty.clients.default.http2.enabled: false
            """;

    private String serverConf = """
                            instance.id: server
                            jetty.client.enabled: false
                            reactivestreams.client.enabled: false
                            jetty.server.http.enabled: false
                            jetty.server.ssl.port: "{{ SYSTEM.port }}"
                            jetty.server.websockets.maxBinaryMessageSize: 1024
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
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                List<Integer> result = new ArrayList<>();
                websocketStreamsServer.subscriber(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        upstream -> upstream.publishOn(Schedulers.immediate(), 1).doOnNext(result::add).subscribe());

                // When
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                List<Integer> source = IntStream.range(0, 10).boxed().toList();
                Flux<Integer> publisher = Flux.fromIterable(source);
                CompletableFuture<Void> publishResult = websocketStreamsClientClient.publish(publisher, serverUri, ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON);
                publishResult.orTimeout(30, TimeUnit.SECONDS).join();

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

                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                List<Integer> result = new ArrayList<>();
                CountDownLatch receivedItem = new CountDownLatch(1);
                CountDownLatch serverCancelled = new CountDownLatch(1);
                websocketStreamsServer.subscriber(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        upstream -> upstream
                                .doOnNext(v -> {
                                    result.add(v);
                                    receivedItem.countDown();
                                }).doOnCancel(serverCancelled::countDown)
                                .subscribe());

                // When
                Sinks.Many<Integer> publisher = Sinks.many().unicast().onBackpressureBuffer(new ArrayBlockingQueue<>(1024));
                CompletableFuture<Void> subscription = websocketStreamsClientClient.publish(publisher.asFlux(),
                        serverUri,
                        ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                );

                logger.info("Emitted: " + publisher.tryEmitNext(42));
                receivedItem.await();
                logger.info("Cancel");
                try {
                    subscription.cancel(true);
                } catch (Throwable e) {
                    logger.error(e);
                }
                serverCancelled.await();
                logger.info("Cancelled on server");


                // Then
                Assertions.assertTrue(subscription.isCancelled());
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

                websocketStreamsServer.subscriber(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        upstream -> upstream
                                .publishOn(Schedulers.immediate(), 1)
                                .handle((v, s) ->
                                {
                                    if (v == 10)
                                        s.error(new IllegalArgumentException("Break"));
                                    else
                                        s.next(v);
                                }).subscribe());

                // When

                // Then
                Assertions.assertThrows(CancellationException.class, () -> {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    List<Integer> source = IntStream.range(0, 10000).boxed().toList();
                    CompletableFuture<Void> result = websocketStreamsClientClient.publish(
                            Flux.fromIterable(source),
                            serverUri,
                            ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                    );
                    result.orTimeout(30, TimeUnit.SECONDS).join();
                });
            }
        }
    }

    @Test
    public void publisherException() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                websocketStreamsServer.subscriber(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        upstream -> upstream
                                .publishOn(Schedulers.immediate(), 1)
                                .subscribe(v -> {}, throwable -> {
                                    System.out.println(throwable);
                                }));

                // When

                // Then
                Assertions.assertThrows(CompletionException.class, () -> {
                    try {
                        URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                        List<Integer> source = IntStream.range(0, 10000).boxed().toList();
                        CompletableFuture<Void> result = websocketStreamsClientClient.publish(
                                Flux.fromIterable(source).<Integer>handle((v, s) ->
                                {
                                    if (v == 10)
                                        s.error(new IllegalArgumentException("Break"));
                                    else
                                        s.next(v);
                                }),
                                serverUri,
                                ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                        );
                        result.orTimeout(30, TimeUnit.SECONDS).join();
                    } catch (Throwable e) {
                        throw e;
                    }
                });
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

                websocketStreamsServer.subscriber(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        upstream -> upstream.doOnNext(System.out::println).subscribe());

                // When

                // Then
                Assertions.assertThrows(CompletionException.class, () ->
                {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();
                    CompletableFuture<Void> result = websocketStreamsClientClient.publish(
                            sink.asFlux(),
                            serverUri,
                            ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                    );
                    result.orTimeout(30, TimeUnit.SECONDS).join();
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

                websocketStreamsServer.subscriber(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        upstream -> upstream.doOnNext(System.out::println).subscribe());

                // When

                // Then
                Assertions.assertThrows(CompletionException.class, () ->
                {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();
                    CompletableFuture<Void> result = websocketStreamsClientClient.publish(
                            sink.asFlux(),
                            serverUri,
                            ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                    );
                    result.orTimeout(30, TimeUnit.SECONDS).join();
                });
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

                AtomicReference<String> resultContext = new AtomicReference<>();
                websocketStreamsServer.subscriber(
                        "numbers/{foo}",
                        ServerWebSocketOptions.instance(),
                        String.class,
                        upstream -> upstream.contextWrite(Context.of("server", "123")).subscribe(resultContext::set));

                // When
                Publisher<String> configPublisher = s -> {
                    if (s instanceof CoreSubscriber<? super String> subscriber) {
                        String val = subscriber.currentContext().stream().filter(e -> !(e.getKey().equals("request") || e.getKey().equals("response"))).toList().toString();
                        s.onSubscribe(new Subscription() {
                            @Override
                            public void request(long n) {
                                subscriber.onNext(val);
                                subscriber.onComplete();
                            }

                            @Override
                            public void cancel() {
                            }
                        });
                    }
                };

                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers/bar?param1=value1");
                Flux<String> publisher = Flux.from(configPublisher).contextWrite(Context.of(
                        "client", "abc"));

                CompletableFuture<Void> result = websocketStreamsClientClient.publish(publisher,
                        serverUri,
                        ClientWebSocketOptions.instance(), String.class, MediaType.APPLICATION_JSON
                );

                result.orTimeout(30, TimeUnit.SECONDS).join();

                // Then
                Assertions.assertEquals("[server=123, foo=bar, param1=value1, client=abc]", resultContext.get());
            }
        }
    }

    @Test
    public void largeItems() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                List<ByteBuffer> result = new ArrayList<>();
                websocketStreamsServer.subscriber(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        ByteBuffer.class,
                        upstream -> upstream.doOnNext(result::add).subscribe());

                // When
                List<ByteBuffer> source = List.of(800, 800, 800)
                        .stream().map(n -> ByteBuffer.wrap(new byte[n]))
                        .toList();

                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");

                CompletableFuture<Void> future = websocketStreamsClientClient.publish(
                        Flux.fromIterable(source),
                        serverUri,
                        ClientWebSocketOptions.instance(), ByteBuffer.class, MediaType.APPLICATION_JSON);

                future.orTimeout(30, TimeUnit.SECONDS).join();

                // Then
                Assertions.assertEquals(source, result);
            }
        }
    }

    @Test
    public void tooLargeItem() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                List<ByteBuffer> result = new ArrayList<>();
                websocketStreamsServer.subscriber(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        ByteBuffer.class,
                        upstream -> upstream.doOnNext(result::add).subscribe());

                // When
                List<ByteBuffer> source = List.of(800, 1800, 800)
                        .stream().map(n -> ByteBuffer.wrap(new byte[n]))
                        .toList();

                // Then
                Assertions.assertThrows(CompletionException.class, () ->
                {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    CompletableFuture<Void> future = websocketStreamsClientClient.publish(
                            Flux.fromIterable(source),
                            serverUri,
                            ClientWebSocketOptions.instance(), ByteBuffer.class, MediaType.APPLICATION_JSON);
                    future.orTimeout(30, TimeUnit.SECONDS).join();
                });
            }
        }
    }
}
