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
package dev.xorcery.reactivestreams.extras.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.net.Sockets;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import dev.xorcery.reactivestreams.extras.loadbalancing.LoadBalancer;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static dev.xorcery.reactivestreams.extras.loadbalancing.LoadBalancer.CONNECTIONS;
import static dev.xorcery.reactivestreams.extras.loadbalancing.LoadBalancer.comparator;

@Disabled("flaky in CI")
public class LoadBalancerTest {


    private String clientConf = """
                            instance.id: client
                            jetty.server.enabled: false
                            reactivestreams.server.enabled: false
                            dns.client.hosts:
                                - name: "_servicetest._sub._https._tcp"
                                  url:
                                    - "wss://localhost:{{ SYSTEM.port1 }}/numbers"
                                    - "wss://localhost:{{ SYSTEM.port2 }}/numbers"
                                    - "wss://localhost:{{ SYSTEM.port3 }}/numbers"
            """;

    private String serverConf = """
                            instance.id: server
                            jetty.client.enabled: false
                            reactivestreams.client.enabled: false
                            jetty.server.http.enabled: false
            """;

    private Configuration clientConfiguration;
    private ContextView webSocketContext;

    Logger logger = LogManager.getLogger();
    List<Xorcery> servers = new ArrayList<>();
    private Xorcery client;

    @BeforeEach
    public void setup() throws Exception {
        int sslPort = Sockets.nextFreePort();
        System.setProperty("port1", Integer.toString(sslPort));
        System.setProperty("port2", Integer.toString(sslPort + 1));
        System.setProperty("port3", Integer.toString(sslPort + 2));

        for (int i = 0; i < 3; i++) {
            Xorcery server = new Xorcery(new ConfigurationBuilder()
                    .addTestDefaults()
                    .addYaml(serverConf)
                    .addYaml("jetty.server.ssl.port: \"{{ SYSTEM.port" + (i + 1) + " }}\"")
                    .build());
            servers.add(server);
        }

        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();

        client = new Xorcery(clientConfiguration);

        webSocketContext = Context.of(ClientWebSocketStreamContext.serverUri.name(), "srv://_servicetest._sub._https._tcp");
    }

    @AfterEach
    public void cleanup() {
        client.close();

        for (Xorcery server : servers) {
            server.close();
        }
    }

    @Test
    public void testLoadBalancer() throws Exception {

        // Start subscriber on each server
        for (Xorcery server : servers) {
            ServerWebSocketStreams websocketStreamsServer = server.getServiceLocator().getService(ServerWebSocketStreams.class);
            List<Integer> result = new ArrayList<>();
            websocketStreamsServer.subscriber(
                    "numbers",
                    ServerWebSocketOptions.instance(),
                    Integer.class,
                    upstream -> upstream.doOnNext(result::add).subscribe());
        }

        // Start publishers on client
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<URI> serverChoices = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 30; i++) {
            ClientWebSocketStreams websocketStreamsClientClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

            CompletableFuture<Void> future = new CompletableFuture<>();
            futures.add(future);
            List<Integer> source = IntStream.range(0, 10000).boxed().toList();
            LoadBalancer loadBalancer = client.getServiceLocator().getService(LoadBalancer.class);

            websocketStreamsClientClient.publishWithResult(Flux.fromIterable(source), null, ClientWebSocketOptions.instance(), Integer.class, Integer.class, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON)
                    .contextWrite(context -> {
                        serverChoices.add(context.get(ClientWebSocketStreamContext.serverUri));
                        return context;
                    })
                    .transformDeferredContextual(loadBalancer.loadBalance(comparator(CONNECTIONS)))
                    .contextWrite(webSocketContext)
                    .doOnError(e -> logger.warn(e))
                    .subscribe(v -> {
                    }, future::completeExceptionally, () -> future.complete(null));
            futures.add(future);
        }

        logger.info("Waiting for publishers to complete");
        for (CompletableFuture<Void> future : futures) {
            future.orTimeout(10, TimeUnit.SECONDS).join();
        }

        logger.info(serverChoices);
    }
}
