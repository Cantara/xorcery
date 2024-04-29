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
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.server.reactor.WebSocketStreamsServerConfiguration;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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
    private WebSocketStreamsServerConfiguration websocketStreamsServerConfiguration;
    private Context webSocketContext;

    Logger logger = LogManager.getLogger();

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        websocketStreamsServerConfiguration = WebSocketStreamsServerConfiguration.get(serverConfiguration);
        webSocketContext = Context.of(ClientWebSocketStreamContext.serverUri.name(), websocketStreamsServerConfiguration.getURI().resolve("numbers"));
    }

    @Test
    public void complete() throws Exception {
        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(serverConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreamsClient websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreamsClient.class);

                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                websocketStreamsServer.publisher(
                        "numbers",
                        Integer.class,
                        Flux.fromIterable(source));

                // When
                Flux<Integer> numbers = websocketStreamsClientClient.subscribe(
                        ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                ).contextWrite(webSocketContext);
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
                ClientWebSocketStreamsClient websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreamsClient.class);

                List<Integer> source = IntStream.range(0, 10000).boxed().toList();
                websocketStreamsServer.publisher(
                        "numbers",
                        Integer.class,
                        Flux.fromIterable(source));

                // When
                List<Integer> result = websocketStreamsClientClient.subscribe(
                                ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                        )
                        .<Integer>handle((v, s) -> {
                            if (v == 10)
                                s.complete();
                            else
                                s.next(v);
                        })
                        .contextWrite(webSocketContext)
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
                ClientWebSocketStreamsClient websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreamsClient.class);

                websocketStreamsServer.publisher(
                        "numbers",
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
                    websocketStreamsClientClient.subscribe(
                                    ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                            )
                            .contextWrite(webSocketContext)
                            .toStream().toList();
                    Assertions.fail();
                } catch (ServerStreamException e) {
                    // Then
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
                ClientWebSocketStreamsClient websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreamsClient.class);

                // When
                websocketStreamsServer.publisher(
                        "numbers",
                        Integer.class,
                        Sinks.many().unicast().<Integer>onBackpressureBuffer().asFlux());

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    List<Integer> result = websocketStreamsClientClient.subscribe(
                                    ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                            )
                            .contextWrite(webSocketContext)
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
                ClientWebSocketStreamsClient websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreamsClient.class);

                websocketStreamsServer.publisher(
                        "numbers",
                        Integer.class,
                        Sinks.many().unicast().<Integer>onBackpressureBuffer().asFlux());

                // When

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    List<Integer> result = websocketStreamsClientClient.subscribe(
                                    ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                            )
                            .contextWrite(webSocketContext)
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
                ClientWebSocketStreamsClient websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreamsClient.class);

                AtomicInteger count = new AtomicInteger(3);

                Sinks.Many<Integer> sink = Sinks.many().replay().all();
                websocketStreamsServer.publisher(
                        "numbers",
                        Integer.class,
                        sink.asFlux());
                IntStream.range(0, 5).boxed().forEach(sink::tryEmitNext);

                // When
                List<Integer> result = websocketStreamsClientClient.subscribe(
                                ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON
                        )
                        .doOnSubscribe(s ->
                        {
                            if (count.decrementAndGet() == 0)
                            {
                                sink.tryEmitComplete();
                            }
                        })
                        .retry()
                        .contextWrite(webSocketContext)
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
                ClientWebSocketStreamsClient websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreamsClient.class);

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
                        String.class,
                        configPublisher);

                String config = websocketStreamsClientClient.subscribe(
                                ClientWebSocketOptions.instance(), String.class, MediaType.APPLICATION_JSON
                        )
                        .contextWrite(Context.of(
                                ClientWebSocketStreamContext.serverUri.name(), websocketStreamsServerConfiguration.getURI().resolve("numbers/bar?param1=value1"),
                                "client", "abc"))
                        .take(1).blockFirst();

                // Then
                Assertions.assertEquals("[foo=bar, client=abc, param1=value1]", config);
            }
        }
    }
}
