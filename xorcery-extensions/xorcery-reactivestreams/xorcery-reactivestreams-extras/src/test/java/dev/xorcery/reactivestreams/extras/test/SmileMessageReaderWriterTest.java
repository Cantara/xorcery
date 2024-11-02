package dev.xorcery.reactivestreams.extras.test;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import dev.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import dev.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import dev.xorcery.reactivestreams.server.ServerWebSocketStreamsConfiguration;
import dev.xorcery.util.Resources;
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
