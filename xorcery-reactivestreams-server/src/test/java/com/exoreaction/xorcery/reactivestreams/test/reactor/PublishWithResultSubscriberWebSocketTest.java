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
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
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
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreams.cancelStream;

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
    public void testPublishWithResultSubscriber() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.map(v -> {
                                    logger.info("Received value " + v);
                                    return v;
                                }
                        ));

                // When
                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                List<Integer> result = websocketStreamsClient.publishWithResult(
                                websocketStreamsServerConfiguration.getURI().resolve("/numbers"),
                                MediaType.APPLICATION_JSON,
                                Integer.class,
                                Integer.class,
                                WebSocketClientOptions.empty(),
                                Flux.fromIterable(source))
                        .toStream().toList();

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

        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.flatMap(v -> Mono.create(sink -> {
                                })
                        ));


                // When
                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                Flux<Integer> destination = websocketStreamsClient.publishWithResult(
                        websocketStreamsServerConfiguration.getURI().resolve("/numbers"),
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        Integer.class,
                        WebSocketClientOptions.empty(),
                        Flux.fromIterable(source));

                // Then
                Assertions.assertThrows(ServerStreamException.class, () ->
                {
                    destination.toStream().toList();
                });
            }
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

        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                websocketStreamsServer.subscriberWithResult(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        Integer.class,
                        upstream -> upstream.handle((v, sink)-> sink.error(new NotAuthorizedStreamException("Not allowed")))
                );


                // When
                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                Flux<Integer> destination = websocketStreamsClient.publishWithResult(
                        websocketStreamsServerConfiguration.getURI().resolve("/numbers"),
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        Integer.class,
                        WebSocketClientOptions.empty(),
                        Flux.fromIterable(source));

                // Then
                Assertions.assertThrows(NotAuthorizedStreamException.class, () ->
                {
                    destination.toStream().toList();
                });
            }
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
                                websocketStreamsServerConfiguration.getURI().resolve("/numbers/bar?param1=value1"),
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
}
