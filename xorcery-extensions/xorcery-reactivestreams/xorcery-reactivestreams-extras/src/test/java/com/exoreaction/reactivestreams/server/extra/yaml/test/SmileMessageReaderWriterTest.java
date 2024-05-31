package com.exoreaction.reactivestreams.server.extra.yaml.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import com.exoreaction.xorcery.reactivestreams.server.ServerWebSocketStreamsConfiguration;
import com.exoreaction.xorcery.util.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

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

        server.getServiceLocator().getService(ServerWebSocketStreams.class)
                .subscriberWithResult(
                        "test",
                        Map.class,
                        Map.class,
                        flux -> flux.doOnNext(map -> map.put("some", "value")));

        YamlPublisher<Map<String, Object>> filePublisher = new YamlPublisher<>(Map.class);
        List<Map<String, Object>> result = Flux.from(filePublisher).transform(client.getServiceLocator().getService(ClientWebSocketStreams.class)
                .<Map<String, Object>, Map<String, Object>>publishWithResult(
                        ClientWebSocketOptions.instance(),
                        Map.class,
                        Map.class,
                        "application/x-jackson-smile",
                        "application/x-jackson-smile"
                ))
                .contextWrite(Context.of(
                        ClientWebSocketStreamContext.serverUri.name(), ServerWebSocketStreamsConfiguration.get(server.getConfiguration()).getURI().resolve("test"),
                        ResourcePublisherContext.resourceUrl.name(), Resources.getResource("testevents.yaml").orElseThrow().toExternalForm()))
                .toStream().toList();

        System.out.println(result);
    }
}
