package com.exoreaction.reactivestreams.server.extra.yaml.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.extras.loadbalancing.LoadBalancer;
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

import static com.exoreaction.xorcery.reactivestreams.extras.loadbalancing.LoadBalancer.CONNECTIONS;
import static com.exoreaction.xorcery.reactivestreams.extras.loadbalancing.LoadBalancer.comparator;

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
                    Integer.class,
                    upstream -> upstream.doOnNext(result::add));
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
            Flux.fromIterable(source)
                    .transform(websocketStreamsClientClient.publish(ClientWebSocketOptions.instance(), Integer.class, MediaType.APPLICATION_JSON))
                    .contextWrite(context -> {
                        serverChoices.add(context.get(ClientWebSocketStreamContext.serverUri));
                        return context;
                    })
                    .transformDeferredContextual(loadBalancer.loadBalance(comparator(CONNECTIONS)))
                    .contextWrite(webSocketContext)
                    .doOnError(e -> logger.warn(e))
                    .subscribe(v -> {
                    }, future::completeExceptionally, () -> future.complete(null));
        }

        logger.info("Waiting for publishers to complete");
        for (CompletableFuture<Void> future : futures) {
            future.orTimeout(10, TimeUnit.SECONDS).join();
        }

        logger.info(serverChoices);
    }
}
