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
package com.exoreaction.xorcery.reactivestreams.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.server.ServerWebSocketStreamsConfiguration;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class StigsPublishSubscriberWebSocketTest {

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
                            jetty.server.websockets.maxBinaryMessageSize: 1024
            """;

    private Configuration clientConfiguration;
    private Configuration serverConfiguration;
    private ServerWebSocketStreamsConfiguration websocketStreamsServerWebSocketStreamsConfiguration;
    private ContextView webSocketContext;

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
                ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
/*
                List<ByteBuffer> source = List.of(800, 800, 800)
                        .stream().map(n -> ByteBuffer.wrap(new byte[n]))
                        .toList();
*/
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
                new Thread(new ThreadedWorker(clientConfiguration, webSocketContext)).start();
                completeLatch.await(30, TimeUnit.SECONDS);

                // Then
                Assertions.assertEquals(source, result);

        }
    }
}

class ThreadedWorker implements Runnable {

    final private Configuration clientConfiguration;
    final private ContextView webSocketContext;

    ThreadedWorker(Configuration clientConfiguration, ContextView webSocketContext) {
        this.clientConfiguration = clientConfiguration;
        this.webSocketContext = webSocketContext;
    }

    @Override
    public void run() {

        try (Xorcery client = new Xorcery(clientConfiguration)) {
            ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

            LogManager.getLogger().info(clientConfiguration);

            Disposable disposable = websocketStreamsClientClient.subscribeWithResult(ClientWebSocketOptions.instance(),
                    Integer.class, Integer.class,
                    List.of(MediaType.APPLICATION_JSON), List.of(MediaType.APPLICATION_JSON),
                    webSocketContext,
                    flux -> flux.map(v ->
                            v //v*v
                    ));
            //Just add some time so not to shut down too early
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Stopped Threaded client");
    }
}