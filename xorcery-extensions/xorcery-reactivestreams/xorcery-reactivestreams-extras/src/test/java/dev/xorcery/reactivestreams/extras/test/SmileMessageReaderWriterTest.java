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

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
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
                        ServerWebSocketOptions.instance(),
                        Map.class,
                        Map.class,
                        flux -> flux.doOnNext(map -> map.put("some", "value")));

        YamlPublisher<Map<String, Object>> filePublisher = new YamlPublisher<>(Map.class);
        List<Map<String, Object>> result = client.getServiceLocator().getService(ClientWebSocketStreams.class)
                .<Map<String, Object>, Map<String, Object>>publishWithResult(
                        Flux.from(filePublisher),
                        ServerWebSocketStreamsConfiguration.get(server.getConfiguration()).getURI().resolve("test"),
                        ClientWebSocketOptions.instance(),
                        Map.class,
                        Map.class,
                        "application/x-jackson-smile",
                        "application/x-jackson-smile"
                )
                .contextWrite(Context.of(
                        ResourcePublisherContext.resourceUrl.name(), Resources.getResource("testevents.yaml").orElseThrow().toExternalForm()))
                .toStream().toList();

        System.out.println(result);
    }
}
