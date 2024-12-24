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
package dev.xorcery.neo4jprojections.test;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.WithMetadata;
import dev.xorcery.neo4j.client.GraphDatabase;
import dev.xorcery.neo4jprojections.SkipEventsUntil;
import dev.xorcery.neo4jprojections.api.Neo4jProjections;
import dev.xorcery.neo4jprojections.api.ProjectionStreamContext;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import dev.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import dev.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import dev.xorcery.reactivestreams.server.ServerWebSocketStreamsConfiguration;
import dev.xorcery.util.Resources;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Neo4jProjectionTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void testProjection(Neo4jProjections neo4jProjections) {
        YamlPublisher<MetadataEvents> filePublisher = new YamlPublisher<>(MetadataEvents.class);
        AtomicInteger timestamp = new AtomicInteger();
        Flux.from(filePublisher)
                .doOnNext(me -> me.metadata().toBuilder().add("timestamp", timestamp.incrementAndGet()))
                .transformDeferredContextual(new SkipEventsUntil(ProjectionStreamContext.projectionPosition.name()))
                .transformDeferredContextual(neo4jProjections.projection())
                .doOnNext(System.out::println)
                .contextWrite(Context.of(
                        ProjectionStreamContext.projectionId.name(), "test",
                        ResourcePublisherContext.resourceUrl.name(), Resources.getResource("events.yaml").orElseThrow()))
                .blockLast();

        GraphDatabase service = xorceryExtension.getServiceLocator().getService(GraphDatabase.class);

        System.out.println(service.query("MATCH (Person:Person)")
                .results(Person.name).build());

        List<JsonNode> result = service.query("MATCH (Person:Person)")
                .results(Person.name)
                .stream(row -> row.getJsonNode("person_name")).toCompletableFuture().join().toList();
        System.out.println(result);

        Map<String, Object> projection = service.getGraphDatabaseService().executeTransactionally("""
                MATCH (projection:Projection)
                RETURN properties(projection) as properties
                """, Map.of(), res ->
                res.hasNext()
                        ? res.next()
                        : Collections.emptyMap());

        System.out.println(projection);
    }

    @Test
    public void testRemoteProjection() {
        // Server
        Neo4jProjections neo4jProjections = xorceryExtension.getServiceLocator().getService(Neo4jProjections.class);
        ServerWebSocketStreams server = xorceryExtension.getServiceLocator().getService(ServerWebSocketStreams.class);
        Disposable subscriber = server.subscriber("projections/{projectionId}", MetadataEvents.class,
                flux -> flux.transformDeferredContextual(neo4jProjections.projection()));

        try {
            // Client
            ClientWebSocketStreams client = xorceryExtension.getServiceLocator().getService(ClientWebSocketStreams.class);

            YamlPublisher<MetadataEvents> filePublisher = new YamlPublisher<>(MetadataEvents.class);
            AtomicInteger timestamp = new AtomicInteger();
            Flux<MetadataEvents> publisher = Flux.from(filePublisher)
                    .doOnNext(me -> me.metadata().toBuilder().add("timestamp", timestamp.incrementAndGet()))
                    .contextWrite(Context.of(ResourcePublisherContext.resourceUrl.name(), Resources.getResource("events.yaml").orElseThrow()));

            URI serverUri = ServerWebSocketStreamsConfiguration.get(xorceryExtension.getConfiguration()).getURI().resolve("projections/testremote");
            publisher.transform(client.publish(ClientWebSocketOptions.instance(), MetadataEvents.class, MediaType.APPLICATION_JSON))
                    .contextWrite(Context.of(ClientWebSocketStreamContext.serverUri.name(), serverUri))
                    .blockLast();

            // Check results
            GraphDatabase service = xorceryExtension.getServiceLocator().getService(GraphDatabase.class);

            System.out.println(service.query("MATCH (Person:Person)")
                    .results(Person.name).build());

            List<JsonNode> result = service.query("MATCH (Person:Person)")
                    .results(Person.name)
                    .stream(row -> row.getJsonNode("person_name")).toCompletableFuture().join().toList();

            System.out.println(result);
        } finally {
            subscriber.dispose();
        }
    }

    @Test
    public void testRemoteProjectionWithObserver() {
        // Server
        Neo4jProjections neo4jProjections = xorceryExtension.getServiceLocator().getService(Neo4jProjections.class);
        ServerWebSocketStreams server = xorceryExtension.getServiceLocator().getService(ServerWebSocketStreams.class);
        Disposable subscriber = server.subscriberWithResult("projections/{projectionId}", MetadataEvents.class, Metadata.class,
                flux -> flux.transformDeferredContextual(neo4jProjections.projection()).map(WithMetadata::metadata));

        try {
            // Client
            ClientWebSocketStreams client = xorceryExtension.getServiceLocator().getService(ClientWebSocketStreams.class);

            YamlPublisher<MetadataEvents> filePublisher = new YamlPublisher<>(MetadataEvents.class);
            AtomicInteger timestamp = new AtomicInteger();
            Flux<MetadataEvents> publisher = Flux.from(filePublisher)
                    .doOnNext(me -> me.metadata().toBuilder().add("timestamp", timestamp.incrementAndGet()))
                    .contextWrite(Context.of(ResourcePublisherContext.resourceUrl.name(), Resources.getResource("events.yaml").orElseThrow()));

            URI serverUri = ServerWebSocketStreamsConfiguration.get(xorceryExtension.getConfiguration()).getURI().resolve("projections/testremote");
            publisher.transform(client.publishWithResult(ClientWebSocketOptions.instance(), MetadataEvents.class, Metadata.class, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON))
                    .contextWrite(Context.of(ClientWebSocketStreamContext.serverUri.name(), serverUri))
                    .blockLast();

            // Check results
            GraphDatabase service = xorceryExtension.getServiceLocator().getService(GraphDatabase.class);

            System.out.println(service.query("MATCH (Person:Person)")
                    .results(Person.name).build());

            List<JsonNode> result = service.query("MATCH (Person:Person)")
                    .results(Person.name)
                    .stream(row -> row.getJsonNode("person_name")).toCompletableFuture().join().toList();

            System.out.println(result);
        } finally {
            subscriber.dispose();
        }
    }

    enum Person {
        name
    }
}
