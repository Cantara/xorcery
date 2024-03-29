package com.exoreaction.reactivestreams.server.extra.yaml.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.extras.publisher.JsonPublisher;
import com.exoreaction.xorcery.reactivestreams.extras.publisher.YamlPublisher;
import com.exoreaction.xorcery.reactivestreams.server.reactor.WebSocketStreamsServerConfiguration;
import com.exoreaction.xorcery.util.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StigsSmilingTest {

    @RegisterExtension
    static XorceryExtension consumer = createXorcery("server", "true");
    @RegisterExtension
    static XorceryExtension producer = createXorcery("client", "false");
    @RegisterExtension
    static XorceryExtension consumer2 = createXorcery("server2", "true");
    @RegisterExtension
    static XorceryExtension producer2 = createXorcery("client2", "false");

    @Test
    public void testSmileMessageReaderWriter() {
        Flux<Map<String, Object>> fluxy = halfOfFlow(consumer, "test", producer2, createYamlPublisher("testevents.yaml"));
        Flux<Map<String, Object>> fluxy2 = halfOfFlow(consumer2, "test2", producer2, createYamlPublisher("testevents2.yaml"));
        Flux<Map<String, Object>> result = Flux.zip(fluxy, fluxy2, (stringObjectMap, stringObjectMap2) -> {
            System.out.println("One " + stringObjectMap);
            System.out.println("Two " + stringObjectMap2);
            System.out.println("------");
            return Map.of();
        });

        List<Map<String, Object>> listy = result.toStream().toList();
        System.out.println("Gots " + listy);
        assertEquals(11, listy.size());
    }

    private static YamlPublisher<Map<String, Object>> createYamlPublisher(String path) {
        return new YamlPublisher<>(Map.class, Resources.getResource(path).orElseThrow());
    }

    private static Flux<Map<String, Object>> halfOfFlow(XorceryExtension cons, String path, XorceryExtension prod, YamlPublisher<Map<String, Object>> filePublisher) {
        cons.getServiceLocator().getService(WebSocketStreamsServer.class)
                .subscriberWithResult(
                        path,
                        Map.class,
                        Map.class,
                        flux -> flux.doOnNext(map -> map.put("some", "value")));

        Flux<Map<String, Object>> fluxy = prod.getServiceLocator().getService(WebSocketStreamsClient.class)
                .publishWithResult(WebSocketStreamsServerConfiguration.get(cons.getConfiguration()).getURI().resolve(path),
                        WebSocketClientOptions.instance(),
                        Map.class,
                        Map.class,
                        filePublisher,
                        "application/json",
                        "application/json"
                );
        return fluxy;
    }

    private static XorceryExtension createXorcery(String server, String isServer) {
        return XorceryExtension.xorcery()
                .id(server)
                .configuration(ConfigurationBuilder::addTestDefaults)
                .addYaml("jetty.server.enabled: " + isServer)
                .build();
    }
}
