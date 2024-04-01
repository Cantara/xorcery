package com.exoreaction.xorcery.neo4jprojections.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.reactor.ProjectionsOperator;
import com.exoreaction.xorcery.neo4jprojections.reactor.SkipEventsUntil;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReactorProjectionTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void testProjection() {
        ProjectionsOperator projectionsOperator = xorceryExtension.getServiceLocator().getService(ProjectionsOperator.class);
        YamlPublisher<MetadataEvents> filePublisher = new YamlPublisher<>(MetadataEvents.class);
        AtomicInteger timestamp = new AtomicInteger();
        Flux.from(filePublisher)
                .doOnNext(me -> me.getMetadata().toBuilder().add("timestamp", timestamp.incrementAndGet()))
                .transformDeferredContextual(new SkipEventsUntil("revision"))
                .map(List::of)
                .transformDeferredContextual(projectionsOperator)
                .doOnNext(System.out::println)
                .contextWrite(Context.of(
                        "projection", "test",
                        ResourcePublisherContext.resourceUrl.name(), Resources.getResource("events.yaml").orElseThrow()))
                .blockLast();

        GraphDatabase service = xorceryExtension.getServiceLocator().getService(GraphDatabase.class);

        System.out.println(service.query("MATCH (Person:Person)")
                .results(Person.name).build());

        List<JsonNode> result = service.query("MATCH (Person:Person)")
                .results(Person.name)
                .stream(row -> row.getJsonNode("person_name")).toCompletableFuture().join().toList();

        System.out.println(result);
    }

    @Test
    public void testRemoteProjection() {
        // Server
        ProjectionsOperator projectionsOperator = xorceryExtension.getServiceLocator().getService(ProjectionsOperator.class);
        WebSocketStreamsServer server = xorceryExtension.getServiceLocator().getService(WebSocketStreamsServer.class);
        server.subscriber("projections/{projection}", MetadataEvents.class,
                flux -> flux.map(List::of).transformDeferredContextual(projectionsOperator).flatMapIterable(v -> v));

        // Client
        WebSocketStreamsClient client = xorceryExtension.getServiceLocator().getService(WebSocketStreamsClient.class);

        YamlPublisher<MetadataEvents> filePublisher = new YamlPublisher<>(MetadataEvents.class);
        AtomicInteger timestamp = new AtomicInteger();
        Flux<MetadataEvents> publisher = Flux.from(filePublisher)
                .doOnNext(me -> me.getMetadata().toBuilder().add("timestamp", timestamp.incrementAndGet()))
                .contextWrite(Context.of(ResourcePublisherContext.resourceUrl.name(), Resources.getResource("events.yaml").orElseThrow()));

        URI serverUri = ReactiveStreamsServerConfiguration.get(xorceryExtension.getConfiguration()).getURI().resolve("projections/test");
        publisher.transform(client.publish(WebSocketClientOptions.instance(), MetadataEvents.class, MediaType.APPLICATION_JSON))
                .contextWrite(Context.of(WebSocketStreamContext.serverUri.name(), serverUri))
                .blockLast();

        // Check results
        GraphDatabase service = xorceryExtension.getServiceLocator().getService(GraphDatabase.class);

        System.out.println(service.query("MATCH (Person:Person)")
                .results(Person.name).build());

        List<JsonNode> result = service.query("MATCH (Person:Person)")
                .results(Person.name)
                .stream(row -> row.getJsonNode("person_name")).toCompletableFuture().join().toList();

        System.out.println(result);
    }

    enum Person {
        name
    }
}
