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
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Timeout(60)
public class SubscribePublisherWebSocketTest {

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
                LogManager.getLogger().info(serverConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                websocketStreamsServer.publisher(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Flux.fromIterable(source));

                // When
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                Flux<Integer> numbers = websocketStreamsClientClient.subscribe(serverUri,
                        ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                );
                List<Integer> result = numbers.toStream().toList();

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

                List<Integer> source = IntStream.range(0, 10000).boxed().toList();
                websocketStreamsServer.publisher(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Flux.fromIterable(source));

                // When
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                List<Integer> result = websocketStreamsClientClient.subscribe(
                                serverUri,ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                        )
                        .<Integer>handle((v, s) -> {
                            if (v == 10)
                                s.complete();
                            else
                                s.next(v);
                        })
                        .toStream().toList();

                // Then
                Assertions.assertEquals(IntStream.range(0, 10).boxed().toList(), result);
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

                websocketStreamsServer.publisher(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Flux.fromStream(IntStream.range(0, 20).boxed()).handle((v, s) ->
                        {
                            if (v == 10)
                                s.error(new IllegalArgumentException("Break"));
                            else
                                s.next(v);
                        }));

                // When
                try {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    websocketStreamsClientClient.subscribe(
                                    serverUri, ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                            )
                            .toStream().toList();
                    Assertions.fail();
                } catch (ServerStreamException e) {
                    // Then
                    Assertions.assertEquals(StatusCode.NORMAL, e.getStatus());
                    Assertions.assertEquals("Break", e.getMessage());
                }
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

                websocketStreamsServer.publisher(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Flux.fromStream(IntStream.range(0, 20).boxed()));

                // When
                try {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    websocketStreamsClientClient.subscribe(
                                    serverUri, ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                            )
                            .handle((v, s) ->
                            {
                                if (v == 10)
                                    s.error(new IllegalArgumentException("Break"));
                                else
                                    s.next(v);
                            })
                            .toStream().toList();
                    Assertions.fail();
                } catch (IllegalArgumentException e) {
                    // Then
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

                // When
                websocketStreamsServer.publisher(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Sinks.many().unicast().<Integer>onBackpressureBuffer().asFlux());

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    List<Integer> result = websocketStreamsClientClient.subscribe(
                                    serverUri, ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                            )
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

                websocketStreamsServer.publisher(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        Sinks.many().unicast().<Integer>onBackpressureBuffer().asFlux());

                // When

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                    List<Integer> result = websocketStreamsClientClient.subscribe(
                                    serverUri, ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                            )
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

                AtomicInteger count = new AtomicInteger(3);

                Sinks.Many<Integer> sink = Sinks.many().replay().all();
                websocketStreamsServer.publisher(
                        "numbers",
                        ServerWebSocketOptions.instance(),
                        Integer.class,
                        sink.asFlux());
                IntStream.range(0, 5).boxed().forEach(sink::tryEmitNext);

                // When
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers");
                List<Integer> result = websocketStreamsClientClient.subscribe(
                                serverUri, ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                        )
                        .doOnSubscribe(s ->
                        {
                            if (count.decrementAndGet() == 0)
                            {
                                sink.tryEmitComplete();
                            }
                        })
                        .retry()
                        .toStream().toList();

                // Then
                Assertions.assertEquals(List.of(0, 1, 2, 3, 4, 0, 1, 2, 3, 4, 0, 1, 2, 3, 4), result);
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
                websocketStreamsServer.publisher(
                        "numbers/{foo}",
                        ServerWebSocketOptions.instance(),
                        String.class,
                        configPublisher);

                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers/bar?param1=value1");
                String config = websocketStreamsClientClient.subscribe(
                                serverUri, ClientWebSocketOptions.instance(), String.class, MediaType.APPLICATION_JSON
                        )
                        .contextWrite(Context.of("client", "abc"))
                        .take(1).blockFirst();

                // Then
                Assertions.assertEquals("[foo=bar, client=abc, param1=value1]", config);
            }
        }
    }
}
