package com.exoreaction.xorcery.neo4jprojections.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.metadata.WithMetadata;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.api.Neo4jProjections;
import com.exoreaction.xorcery.neo4jprojections.api.ProjectionStreamContext;
import com.exoreaction.xorcery.neo4jprojections.SkipEventsUntil;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
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

            URI serverUri = ReactiveStreamsServerConfiguration.get(xorceryExtension.getConfiguration()).getURI().resolve("projections/testremote");
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

            URI serverUri = ReactiveStreamsServerConfiguration.get(xorceryExtension.getConfiguration()).getURI().resolve("projections/testremote");
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
