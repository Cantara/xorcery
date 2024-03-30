package com.exoreaction.reactivestreams.server.extra.yaml.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.extras.publisher.YamlPublisher;
import com.exoreaction.xorcery.reactivestreams.server.reactor.WebSocketStreamsServerConfiguration;
import com.exoreaction.xorcery.util.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

public class SmileMessageReaderWriterTest {

    @RegisterExtension
    static XorceryExtension server = XorceryExtension.xorcery()
            .id("server")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    jetty.server.enabled: true
                    """)
            .build();

    @RegisterExtension
    static XorceryExtension client = XorceryExtension.xorcery()
            .id("client")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    jetty.server.enabled: false
                    """)
            .build();

    @Test
    public void testSmileMessageReaderWriter() {

        server.getServiceLocator().getService(WebSocketStreamsServer.class)
                .subscriberWithResult(
                        "test",
                        Map.class,
                        Map.class,
                        flux -> flux.doOnNext(map -> map.put("some", "value")));

        YamlPublisher<Map<String, Object>> filePublisher = new YamlPublisher<>(Map.class, Resources.getResource("testevents.yaml").orElseThrow());
        List<Map<String, Object>> result = client.getServiceLocator().getService(WebSocketStreamsClient.class)
                .<Map<String, Object>, Map<String, Object>>publishWithResult(WebSocketStreamsServerConfiguration.get(server.getConfiguration()).getURI().resolve("test"),
                        WebSocketClientOptions.instance(),
                        Map.class,
                        Map.class,
                        filePublisher,
                        "application/x-jackson-smile",
                        "application/x-jackson-smile"
                ).toStream().toList();

        System.out.println(result);
    }
}
