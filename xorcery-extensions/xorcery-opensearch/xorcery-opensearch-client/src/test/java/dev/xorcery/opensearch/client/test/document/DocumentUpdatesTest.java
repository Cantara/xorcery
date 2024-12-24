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
package dev.xorcery.opensearch.client.test.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.opensearch.OpenSearchService;
import dev.xorcery.opensearch.client.search.Document;
import dev.xorcery.opensearch.client.search.SearchQuery;
import dev.xorcery.opensearch.client.search.SearchRequest;
import dev.xorcery.opensearch.client.search.SearchResponse;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.util.UUIDs;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;

//@Disabled
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentUpdatesTest {

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yaml"))
                    .withLogConsumer("opensearch", new Slf4jLogConsumer(LoggerFactory.getLogger(DocumentUpdatesTest.class)))
                    .withExposedService("opensearch", 9200, Wait.forListeningPorts(9200))
                    .withStartupTimeout(Duration.ofMinutes(10));

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void testBulkUpdateHandler(OpenSearchService openSearchService, Logger logger) throws InterruptedException {
        Flux.fromStream(IntStream.range(0, 10000).boxed())
                .map(i ->
                        new MetadataJsonNode<JsonNode>(new Metadata.Builder()
                                .add("timestamp", System.currentTimeMillis())
                                .build(),
                                JsonNodeFactory.instance.objectNode().put("number", i)))
                .transformDeferredContextual(openSearchService.documentUpdates(metadataJson -> UUIDs.newId(), Function.identity()))
                .contextWrite(Context.of("index", "numbers", "host", "http://localhost:9200"))
                .blockLast();

        // Wait to allow documents to be indexed properly
        Thread.sleep(5000);

        SearchResponse response = openSearchService.getClient().search().search("numbers", SearchRequest.builder()
                .query(SearchQuery.match_all()).build(), Map.of("size", "1", "sort", "@timestamp:desc"))
                .orTimeout(10, TimeUnit.SECONDS).join();

        for (Document document : response.hits().documents()) {
            System.out.println(document.source().toPrettyString());
        }
    }
}
