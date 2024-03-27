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
package com.exoreaction.reactivestreams.server.extra.yaml.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.extras.publisher.YamlPublisher;
import com.exoreaction.xorcery.reactivestreams.server.reactor.WebSocketStreamsServerConfiguration;
import com.exoreaction.xorcery.util.Resources;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class YamlPublisherTest {

    private Configuration clientConfiguration;

    private Configuration serverConfiguration;
    private WebSocketStreamsServerConfiguration websocketStreamsServerConfiguration;

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
            """;

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        websocketStreamsServerConfiguration = WebSocketStreamsServerConfiguration.get(serverConfiguration);
    }

    @Test
    public void testYamlPublisher() throws Exception {

        YamlPublisher<Map<String, Object>> filePublisher = new YamlPublisher<>(Map.class, Resources.getResource("testevents.yaml").orElseThrow());

        List<Map<String, Object>> result = Flux
                .from(filePublisher)
                .toStream()
                .toList();

        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(clientConfiguration);
                WebSocketStreamsServer websocketStreamsServer = server.getServiceLocator().getService(WebSocketStreamsServer.class);
                WebSocketStreamsClient websocketStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);

                List<Integer> xorceryResult = new ArrayList<>();
                websocketStreamsServer.subscriber(
                        "numbers",
                        MediaType.APPLICATION_JSON,
                        Integer.class,
                        upstream -> upstream.doOnNext(xorceryResult::add));

                // When
                List<Integer> source = IntStream.range(0, 100).boxed().toList();
                websocketStreamsClient.publish(
                                websocketStreamsServerConfiguration.getURI().resolve("numbers"),
                                MediaType.APPLICATION_JSON,
                                Integer.class,
                                WebSocketClientOptions.instance(),
                                Flux.fromIterable(source))
                        .blockLast();

                // Then
                Assertions.assertEquals(source, xorceryResult);
            }
        }


        System.out.println(result);

    }
}
