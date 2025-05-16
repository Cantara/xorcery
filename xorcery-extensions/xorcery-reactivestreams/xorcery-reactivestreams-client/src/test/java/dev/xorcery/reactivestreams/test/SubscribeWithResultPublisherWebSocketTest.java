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
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.api.server.ServerStreamException;
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

import java.net.URI;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions.instance;

@Timeout(60)
public class SubscribeWithResultPublisherWebSocketTest {

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
    private Context webSocketContext;

    Logger logger = LogManager.getLogger();

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        websocketStreamsServerWebSocketStreamsConfiguration = ServerWebSocketStreamsConfiguration.get(serverConfiguration);
        webSocketContext = Context.of(ClientWebSocketStreamContext.serverUri.name(), websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers"));
    }

    @Test
    public void complete() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
                ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

                List<Integer> source = IntStream.range(0, 30).boxed().toList();
                List<Integer> result = new CopyOnWriteArrayList<>();

                CountDownLatch completeLatch = new CountDownLatch(1);
                websocketStreamsServer.publisherWithResult(
                        "numbers",
                        Integer.class,
                        Integer.class,
                        upstream -> {
                            upstream.doOnTerminate(completeLatch::countDown)
                                    .subscribe(result::add);
                            return Flux.fromIterable(source);
                        });

                // When
                Disposable disposable = websocketStreamsClientClient.subscribeWithResult(instance(),
                        Integer.class, Integer.class,
                        webSocketContext,
                        flux -> flux.map(v -> v));

                completeLatch.await(30, TimeUnit.SECONDS);

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

                List<Integer> source = IntStream.range(0, 30000).boxed().toList();
                List<Integer> result = new CopyOnWriteArrayList<>();
                CountDownLatch complete = new CountDownLatch(1);
                websocketStreamsServer.publisherWithResult(
                        "numbers",
                        Integer.class,
                        Integer.class,
                        upstream -> {
                            upstream.doOnTerminate(complete::countDown)
                                    .subscribe(result::add);
                            return Flux.fromIterable(source);
                        });

                // When
                websocketStreamsClientClient.subscribeWithResult(instance(), Integer.class, Integer.class, webSocketContext,
                        flux -> flux.handle((v, sink) ->
                        {
                            if (v == 3) {
                                sink.complete();
                            } else {
                                sink.next(v);
                            }
                        }));

                complete.await(10, TimeUnit.SECONDS);

                // Then
                Assertions.assertEquals(List.of(0, 1, 2), result);
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


                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                websocketStreamsServer.publisherWithResult(
                        "numbers",
                        Integer.class,
                        Integer.class,
                        upstream -> {
                            upstream.subscribe();
                            return Flux.fromIterable(source).handle((v, s) ->
                            {
                                if (v == 10)
                                    s.error(new IllegalArgumentException("Break"));
                                else
                                    s.next(v);
                            });
                        });

                // Then
                try {
                    CompletableFuture<Void> clientResult = new CompletableFuture<>();
                    Disposable disposable = websocketStreamsClientClient.subscribeWithResult(instance(), Integer.class, Integer.class, webSocketContext,
                            flux -> flux.map(v -> v)
                                    .doOnComplete(() -> clientResult.complete(null))
                                    .doOnError(clientResult::completeExceptionally));
                    clientResult.orTimeout(30, TimeUnit.SECONDS).join();
                    Assertions.fail();
                } catch (CompletionException e) {
                    if (e.getCause() instanceof ServerStreamException sse) {
                        Assertions.assertEquals(500, sse.getStatus());
                        Assertions.assertEquals("Break", sse.getMessage());
                    } else {
                        Assertions.fail("Wrong exception");
                    }
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

                websocketStreamsServer.publisherWithResult(
                        "numbers",
                        Integer.class,
                        Integer.class,
                        upstream -> {
                            upstream.subscribe();
                            return Sinks.many().unicast().<Integer>onBackpressureBuffer().asFlux();
                        });

                // When

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    websocketStreamsClientClient.subscribeWithResult(instance(), Integer.class, Integer.class, webSocketContext,
                            flux -> flux.doOnError(result::completeExceptionally));

                    try {
                        result.orTimeout(30, TimeUnit.SECONDS).join();
                    } catch (Exception e) {
                        throw e.getCause();
                    }
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

                websocketStreamsServer.publisherWithResult(
                        "numbers",
                        Integer.class,
                        Integer.class,
                        upstream -> {
                            upstream.subscribe();
                            return Sinks.many().unicast().<Integer>onBackpressureBuffer().asFlux();
                        });

                // When

                // Then
                Assertions.assertThrows(IdleTimeoutStreamException.class, () ->
                {
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    websocketStreamsClientClient.subscribeWithResult(instance(), Integer.class, Integer.class, webSocketContext,
                            flux -> flux.doOnError(result::completeExceptionally));

                    try {
                        result.orTimeout(30, TimeUnit.SECONDS).join();
                    } catch (Exception e) {
                        throw e.getCause();
                    }
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

                AtomicReference<String> result = new AtomicReference<>();
                Publisher<String> configPublisher = s -> {
                    if (s instanceof CoreSubscriber<? super String> subscriber) {
                        String val = subscriber.currentContext().stream().filter(e -> !(e.getKey().equals("request") || e.getKey().equals("response"))).toList().toString();
                        result.set(val);
                        s.onSubscribe(new Subscription() {
                            @Override
                            public void request(long n) {
                                subscriber.onNext(val);
                                subscriber.onComplete();
                            }

                            @Override
                            public void cancel() {
                                subscriber.onComplete();
                            }
                        });
                    }
                };

                websocketStreamsServer.publisherWithResult(
                        "numbers/{foo}",
                        String.class,
                        String.class,
                        upstream -> {
                            upstream.contextWrite(Context.of("server", "123")).subscribe();
                            return configPublisher;
                        });

                // When

                CompletableFuture<Void> done = new CompletableFuture<>();
                URI serverUri = websocketStreamsServerWebSocketStreamsConfiguration.getURI().resolve("numbers/bar?param1=value1");
                Disposable disposable = websocketStreamsClientClient.subscribeWithResult(instance(), String.class, String.class,
                                Context.of(
                                        ClientWebSocketStreamContext.serverUri.name(), serverUri,
                                        "client", "abc"),
                                flux -> flux.doOnComplete(()->done.complete(null))
                                );

                done.orTimeout(30, TimeUnit.SECONDS).join();
                // Then
// TODO Server cannot provide context, see if this is fixable Assertions.assertEquals(String.format("[server=123, serverUri=%s, foo=bar, client=abc, param1=value1]", serverUri.toASCIIString()), result.get());
                Assertions.assertEquals(String.format("[serverUri=%s, foo=bar, client=abc, param1=value1]", serverUri.toASCIIString()), result.get());
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
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.map(v -> v));

                // When
                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                List<Integer> result = Flux.fromIterable(source)
                        .transform(websocketStreamsClientClient.publishWithResult(ClientWebSocketOptions.instance(), Integer.class, Integer.class))
                        .contextWrite(webSocketContext)
                        .toStream().toList();

                // Then
                Assertions.assertEquals(source, result);
            }
        }
    }

}
