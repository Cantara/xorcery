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
import com.exoreaction.xorcery.reactivestreams.api.IdleTimeoutStreamException;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import com.exoreaction.xorcery.reactivestreams.server.reactor.WebSocketStreamsServerConfiguration;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;
import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreams.cancelStream;

public class PublishSubscriberWebSocketTest {

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
    private WebSocketStreamsServerConfiguration websocketStreamsServerConfiguration;

    Logger logger = LogManager.getLogger();

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        websocketStreamsServerConfiguration = WebSocketStreamsServerConfiguration.get(serverConfiguration);
    }

    @Test
    public void complete() throws Exception {

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
                                WebSocketClientOptions.instance(),
                                Flux.fromIterable(source))
                        .blockLast();

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
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                List<Integer> result = new ArrayList<>();
                CountDownLatch latch = new CountDownLatch(1);
                websocketStreamsServer.subscriber(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        upstream -> upstream
                                .doOnNext(v -> {
                                    result.add(v);
                                    latch.countDown();
                                }));

                // When
                Sinks.Many<Integer> publisher = Sinks.many().unicast().onBackpressureBuffer(new ArrayBlockingQueue<>(1024));
                Disposable subscription = websocketStreamsClient.publish(
                                websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                MediaType.APPLICATION_JSON,
                                Integer.class,
                                WebSocketClientOptions.instance(),
                                publisher.asFlux())
                        .subscribe();

                logger.info("Emitted: " + publisher.tryEmitNext(42));
                latch.await();
                subscription.dispose();
                logger.info("Disposed");

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
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                websocketStreamsServer.subscriber(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        upstream -> upstream.handle((v, s) ->
                        {
                            if (v == 10)
                                s.error(new IllegalArgumentException("Break"));
                            else
                                s.next(v);
                        }));

                // When

                // Then
                try {
                    List<Integer> source = IntStream.range(0, 100).boxed().toList();
                    websocketStreamsClient.publish(
                                    websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                    MediaType.APPLICATION_JSON,
                                    Integer.class,
                                    WebSocketClientOptions.instance(),
                                    Flux.fromIterable(source))
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
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                websocketStreamsServer.subscriber(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        upstream -> upstream.doOnNext(System.out::println));

                // When

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();
                    List<Integer> result = websocketStreamsClient.publish(
                                    websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                    MediaType.APPLICATION_JSON,
                                    Integer.class,
                                    WebSocketClientOptions.instance(),
                                    sink.asFlux().doOnNext(System.out::println))
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
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                websocketStreamsServer.subscriber(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        upstream -> upstream.doOnNext(System.out::println));

                // When

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();
                    List<Integer> result = websocketStreamsClient.publish(
                                    websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                    MediaType.APPLICATION_JSON,
                                    Integer.class,
                                    WebSocketClientOptions.instance(),
                                    sink.asFlux())
                            .toStream().toList();
                });
            }
        }
    }

    @Test
    @Disabled()
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
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                List<Integer> result = new ArrayList<>();
                CountDownLatch latch = new CountDownLatch(1);
                websocketStreamsServer.subscriber(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        upstream -> upstream.doOnNext(result::add).doOnComplete(latch::countDown));

                // When
                Sinks.Many<Integer> sink = Sinks.many().multicast().onBackpressureBuffer();
                websocketStreamsClient.publish(
                                websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                MediaType.APPLICATION_JSON,
                                Integer.class,
                                WebSocketClientOptions.instance(),
                                sink.asFlux())
                        .retry()
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
                Assertions.assertEquals(List.of(0,1,2,3,4), result);
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
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                websocketStreamsServer.subscriber(
                        "numbers/{foo}",
                        MediaType.APPLICATION_JSON,
                        String.class,
                        upstream -> upstream.contextWrite(Context.of("server", "123")));

                // When
                Publisher<String> configPublisher = s -> {
                    if (s instanceof CoreSubscriber<? super String> subscriber) {
                        String val = subscriber.currentContext().stream().filter(e -> !(e.getKey().equals("request")||e.getKey().equals("response"))).toList().toString();
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

                String config = websocketStreamsClient.publish(
                                websocketStreamsServerConfiguration.getURI().resolve("/numbers/bar?param1=value1"),
                                MediaType.APPLICATION_JSON,
                                String.class,
                                WebSocketClientOptions.instance(),
                                configPublisher)
                        .contextWrite(Context.of("client", "abc"))
                        .take(1).blockFirst();

                // Then
                Assertions.assertEquals("[server=123, foo=bar, client=abc, param1=value1]", config);
            }
        }
    }
}
