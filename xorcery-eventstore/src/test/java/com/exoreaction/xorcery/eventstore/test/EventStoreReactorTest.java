package com.exoreaction.xorcery.eventstore.test;

import com.eventstore.dbclient.AppendToStreamOptions;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.eventstore.reactor.AppendMetadataByteBuffers;
import com.exoreaction.xorcery.eventstore.reactor.EventStoreClient;
import com.exoreaction.xorcery.eventstore.reactor.EventStoreCommit;
import com.exoreaction.xorcery.eventstore.reactor.MetadataByteBuffer;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.extras.publisher.YamlPublisher;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EventStoreReactorTest {

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yaml"))
                    .withLogConsumer("eventstore", new Slf4jLogConsumer(LoggerFactory.getLogger(EventStoreTest.class)))
                    .withExposedService("eventstore", 2115, Wait.forListeningPorts(2115))
                    .withStartupTimeout(Duration.ofMinutes(10));

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    private static EventStoreDBClientSettings settings;
    private static EventStoreClient client;
    private static JsonMapper jsonMapper = new JsonMapper();

    @BeforeAll
    public static void setup() {
        settings = EventStoreDBConnectionString.parseOrThrow("esdb://localhost:2115?tls=false");
        client = xorcery.getServiceLocator().getService(EventStoreClient.Factory.class).create(settings);
    }

    @Test
    @Order(1)
    public void appender() {
        URL yamlResource = Resources.getResource("testevents1.yaml").orElseThrow();
        YamlPublisher<ObjectNode> filePublisher = new YamlPublisher<>(ObjectNode.class, yamlResource);
        List<EventStoreCommit> result = Flux
                .from(filePublisher)
                .handle(toAppendMetadataByteBuffers("test-eventstream"))
                .handle(client.appender(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        AppendToStreamOptions.get().deadline(30000)))
                .toStream()
                .toList();

        System.out.println(result);
    }

    @Test
    @Order(2)
    public void resumeFromLastPosition() {
        URL yamlResource = Resources.getResource("testevents2.yaml").orElseThrow();
        YamlPublisher<ObjectNode> filePublisher = new YamlPublisher<>(ObjectNode.class, yamlResource);
        List<EventStoreCommit> result = Flux
                .from(filePublisher)
                .contextWrite(client.lastPosition("test-eventstream"))
                .handle(toAppendMetadataByteBuffers("test-eventstream"))
                .retry(10)
                .handle(client.appender(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        AppendToStreamOptions.get().deadline(30000)))
                .toStream()
                .toList();

        System.out.println(result);
    }

    @Test
    @Order(3)
    public void readStream() {
        List<MetadataByteBuffer> result = Flux.from(client.readStream("test-eventstream"))
                .toStream()
                .toList();

        System.out.println(result);
    }

    @Test
    @Order(4)
    public void readStreamSubscription() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        List<MetadataByteBuffer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        Flux.from(client.readStreamSubscription("test-eventstream"))
                .doOnNext(result::add)
                .<MetadataByteBuffer>handle((val, sink) ->
                {
                    val.metadata().getString("timestamp").ifPresent(System.out::println);
                    if (counter.incrementAndGet() == 11) {
                        sink.complete();
                        latch.countDown();
                        System.out.println("DONE!");
                    } else
                        sink.next(val);
                })
                .doOnError(System.out::println)
                .subscribe();
        latch.await(5, TimeUnit.SECONDS);

        System.out.println(result);
    }

    @Test
    @Order(5)
    public void appendAndRead() throws InterruptedException {
        // Append events from file to a stream
        URL yamlResource = Resources.getResource("testevents2.yaml").orElseThrow();
        YamlPublisher<ObjectNode> filePublisher = new YamlPublisher<>(ObjectNode.class, yamlResource);
        Disposable appender = Flux
                .from(filePublisher)
                .contextWrite(client.lastPosition("test-eventstream2"))
                .handle(toAppendMetadataByteBuffers("test-eventstream2"))
                .retry(10)
                .handle(client.appender(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        AppendToStreamOptions.get().deadline(30000)))
                .subscribe();

        // Read events from projection
        AtomicInteger counter = new AtomicInteger();
        List<String> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        Flux.from(client.readStreamSubscription("$ce-test"))
                .map(val -> val.metadata().getString("timestamp").get())
                .doOnNext(result::add)
                .<String>handle((val, sink) ->
                {
                    if (counter.incrementAndGet() == 22) {
                        sink.complete();
                        latch.countDown();
                        System.out.println("DONE!");
                    } else
                        sink.next(val);
                })
                .doOnError(System.out::println)
                .subscribe();
        latch.await(5, TimeUnit.SECONDS);

        appender.dispose();

        System.out.println(result);
    }

    @Test
    @Order(6)
    public void idempotentAppend() {
        URL yamlResource = Resources.getResource("testevents1.yaml").orElseThrow();
        YamlPublisher<ObjectNode> filePublisher = new YamlPublisher<>(ObjectNode.class, yamlResource);
        UUID fixedId = UUID.randomUUID();
        List<Long> result = Flux
                .from(filePublisher)
                .handle(toAppendMetadataByteBuffers("test-eventstream3"))
                .retry(10)
                .handle(client.appender(
                        metadata -> fixedId,
                        metadata -> "DomainCommand",
                        AppendToStreamOptions.get().deadline(30000)))
                .toStream()
                .map(EventStoreCommit::position)
                .toList();

        System.out.println(result);

        Assertions.assertEquals(List.of(0L, 0L, 0L), result);
    }


    private <T> BiConsumer<T, SynchronousSink<T>> skipUntilPosition() {
        AtomicLong counter = new AtomicLong();
        return (value, sink) ->
        {
            if (sink.contextView().getOrDefault("position", null) instanceof Long position) {
                System.out.println(counter + " " + position);
                if (counter.getAndIncrement() > position) {
                    System.out.println("Include");
                    sink.next(value);
                } else {
                    System.out.println("Skip");
                }
            }
        };
    }

    private static BiConsumer<ObjectNode, SynchronousSink<AppendMetadataByteBuffers>> toAppendMetadataByteBuffers(String streamName) {
        return (json, sink) ->
        {
            try {
                ByteBuffer byteBuffer = ByteBuffer.wrap(jsonMapper.writeValueAsBytes(json.get("event")));
                sink.next(new AppendMetadataByteBuffers(streamName, List.of(new MetadataByteBuffer(new Metadata((ObjectNode) json.get("metadata")), byteBuffer))));
            } catch (JsonProcessingException e) {
                sink.error(e);
            }
        };
    }
}
