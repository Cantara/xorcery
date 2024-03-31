package com.exoreaction.xorcery.eventstore.client.test;

import com.eventstore.dbclient.DeleteResult;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.eventstore.client.EventStoreContext;
import com.exoreaction.xorcery.eventstore.client.api.AppendMetadataByteBuffers;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreClient;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreCommit;
import com.exoreaction.xorcery.eventstore.client.api.MetadataByteBuffer;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.extras.publisher.YamlPublisher;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EventStoreClientTest {

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yaml"))
                    .withLogConsumer("eventstore", new Slf4jLogConsumer(LoggerFactory.getLogger(EventStoreClientTest.class)))
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
                .transformDeferredContextual(toAppendMetadataByteBuffers())
                .handle(client.appender(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null))
                .contextWrite(Context.of(EventStoreContext.streamId.name(), "test-eventstream"))
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
                .transformDeferredContextual(client.lastPosition(null))
                .transformDeferredContextual(toAppendMetadataByteBuffers())
                .retry(10)
                .handle(client.appender(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null))
                .contextWrite(Context.of(EventStoreContext.streamId.name(), "test-eventstream"))
                .toStream()
                .toList();

        System.out.println(result);
    }

    @Test
    @Order(3)
    public void readStream() {

        {
            List<MetadataByteBuffer> result = Flux.from(client.readStream("test-eventstream"))
                    .toStream()
                    .toList();
            System.out.println(result);
            Assertions.assertEquals(14, result.size());
        }

        // Name as context
        {
            List<MetadataByteBuffer> result = Flux.from(client.readStream(null))
                    .contextWrite(Context.of(EventStoreContext.streamId.name(), "test-eventstream"))
                    .toStream()
                    .toList();

            System.out.println(result);
            Assertions.assertEquals(14, result.size());
        }
    }

    @Test
    @Order(4)
    public void readStreamSubscription() throws InterruptedException {
        {
            AtomicInteger counter = new AtomicInteger();
            List<MetadataByteBuffer> result = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            Disposable disposable = Flux.from(client.readStreamSubscription("test-eventstream"))
                    .<MetadataByteBuffer>handle((val, sink) ->
                    {
                        val.metadata().getString("timestamp").ifPresent(System.out::println);
                        if (counter.incrementAndGet() >= 14) {
                            sink.complete();
                            latch.countDown();
                            System.out.println("DONE!");
                        } else
                            sink.next(val);
                    })
                    .doOnNext(result::add)
                    .doOnError(System.out::println)
                    .subscribe();
            latch.await(10, TimeUnit.SECONDS);
            disposable.dispose();

            System.out.println(result);
        }

        {
            AtomicInteger counter = new AtomicInteger();
            List<MetadataByteBuffer> result = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            Disposable disposable = Flux.from(client.readStreamSubscription(null))
                    .<MetadataByteBuffer>handle((val, sink) ->
                    {
                        val.metadata().getString("timestamp").ifPresent(System.out::println);
                        if (counter.incrementAndGet() >= 14) {
                            sink.complete();
                            latch.countDown();
                            System.out.println("DONE!");
                        } else
                            sink.next(val);
                    })
                    .doOnNext(result::add)
                    .doOnError(System.out::println)
                    .contextWrite(Context.of(EventStoreContext.streamId.name(), "test-eventstream"))
                    .subscribe();
            latch.await(10, TimeUnit.SECONDS);
            disposable.dispose();

            System.out.println(result);
        }
    }

    @Test
    @Order(5)
    public void appendAndRead() throws InterruptedException {
        // Append events from file to a streamId
        URL yamlResource = Resources.getResource("testevents2.yaml").orElseThrow();
        YamlPublisher<ObjectNode> filePublisher = new YamlPublisher<>(ObjectNode.class, yamlResource);
        Disposable appender = Flux
                .from(filePublisher)
                .transformDeferredContextual(client.lastPosition(null))
                .transformDeferredContextual(toAppendMetadataByteBuffers())
                .retry(10)
                .handle(client.appender(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null))
                .contextWrite(Context.of(EventStoreContext.streamId.name(), "test-eventstream2"))
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
        URL yamlResource = Resources.getResource("testevents2.yaml").orElseThrow();
        YamlPublisher<ObjectNode> filePublisher = new YamlPublisher<>(ObjectNode.class, yamlResource);
        UUID fixedId = UUID.randomUUID();
        AtomicInteger counter = new AtomicInteger();
        List<Long> result = Flux
                .from(filePublisher)
                .transformDeferredContextual(toAppendMetadataByteBuffers())
                .retry(10)
                .handle(client.appender(
                        metadata -> counter.incrementAndGet() >= 3 && counter.get() <= 6 ? fixedId : UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null))
                .contextWrite(Context.of(EventStoreContext.streamId.name(), "test-eventstream3"))
                .toStream()
                .map(EventStoreCommit::streamPosition)
                .toList();

        System.out.println(result);

        Assertions.assertEquals(List.of(0L, 1L, 2L, 2L, 2L, 2L, 3L, 4L, 5L, 6L, 7L), result);
    }

    @Test
    public void optimisticLockingAppend() {

        // Make first commit
        EventStoreCommit commit = Mono.just(new AppendMetadataByteBuffers("domainevents-1234", null,
                        List.of(new MetadataByteBuffer(new Metadata.Builder().build(), ByteBuffer.wrap("eventdata".getBytes(StandardCharsets.UTF_8))))))
                .handle(client.appender(metadata -> UUID.randomUUID(), metadata -> "DomainCommand", null))
                .toFuture().orTimeout(10, TimeUnit.SECONDS).join();

        // Make second commit, and now we know what position to expect in EventStore
        EventStoreCommit commit2 = Mono.just(new AppendMetadataByteBuffers("domainevents-1234", commit.streamPosition(),
                        List.of(new MetadataByteBuffer(new Metadata.Builder().build(), ByteBuffer.wrap("eventdata".getBytes(StandardCharsets.UTF_8))))))
                .handle(client.appender(metadata -> UUID.randomUUID(), metadata -> "DomainCommand", null))
                .toFuture().orTimeout(10, TimeUnit.SECONDS).join();

        // Make third commit against same position, should fail
        Assertions.assertThrows(CompletionException.class, () ->
        {
            EventStoreCommit commit3 = Mono.just(new AppendMetadataByteBuffers("domainevents-1234", commit.streamPosition(),
                            List.of(new MetadataByteBuffer(new Metadata.Builder().build(), ByteBuffer.wrap("eventdata".getBytes(StandardCharsets.UTF_8))))))
                    .handle(client.appender(metadata -> UUID.randomUUID(), metadata -> "DomainCommand", null))
                    .toFuture().orTimeout(10, TimeUnit.SECONDS).join();
        });
    }

    @Test
    public void deleteProjectedStream() {
        URL yamlResource = Resources.getResource("testevents1.yaml").orElseThrow();
        YamlPublisher<ObjectNode> filePublisher = new YamlPublisher<>(ObjectNode.class, yamlResource);
        System.out.println("Append 1");
        Flux.from(filePublisher)
                .transformDeferredContextual(toAppendMetadataByteBuffers())
                .handle(client.appender(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null))
                .contextWrite(Context.of(EventStoreContext.streamId.name(), "testdelete-eventstream"))
                .blockLast();

        System.out.println("Append 2");
        Flux.from(filePublisher)
                .transformDeferredContextual(toAppendMetadataByteBuffers())
                .handle(client.appender(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null))
                .contextWrite(Context.of(EventStoreContext.streamId.name(), "testdelete-eventstream2-foo"))
                .blockLast();

        System.out.println("Read 1");
        List<MetadataByteBuffer> before = Flux.from(client.readStream("$ce-testdelete"))
                .toStream()
                .toList();
        System.out.println(before);

        System.out.println("Delete ");
        Flux.from(client.readStream("$streams"))
                .handle((value, sink) ->
                {
                    value.metadata().getString(EventStoreContext.originalStreamId).ifPresent(streamId ->
                    {
                        if (streamId.startsWith("testdelete-eventstream2")) {
                            System.out.println("Delete "+streamId);
                            DeleteResult deleteResult = client.getClient().deleteStream(streamId).orTimeout(10, TimeUnit.SECONDS).join();
                        }
                    });
                })
                .blockLast();


        System.out.println("Read 2");
        List<MetadataByteBuffer> after = Flux.from(client.readStream("$ce-testdelete"))
                .toStream()
                .toList();
        System.out.println(after);
        Assertions.assertEquals(3, after.size());

        System.out.println("Streams");
        List<MetadataByteBuffer> streams = Flux.from(client.readStream("$streams"))
                .filter(value -> value.metadata().getString(EventStoreContext.originalStreamId).map(id -> id.startsWith("testdelete")).orElse(false))
                .toStream()
                .toList();
        System.out.println(streams);
        Assertions.assertEquals(1, streams.size());
    }

    private <T> BiConsumer<T, SynchronousSink<T>> skipUntilPosition() {
        AtomicLong counter = new AtomicLong();
        return (value, sink) ->
        {
            if (sink.contextView().getOrDefault("streamPosition", null) instanceof Long position) {
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

    private static BiFunction<Flux<ObjectNode>, ContextView, Publisher<AppendMetadataByteBuffers>> toAppendMetadataByteBuffers() {
        return (flux, context) -> flux.handle((json, sink) ->
        {
            try {
                ByteBuffer byteBuffer = ByteBuffer.wrap(jsonMapper.writeValueAsBytes(json.get("event")));
                sink.next(new AppendMetadataByteBuffers(context.get(EventStoreContext.streamId.name()), null, List.of(new MetadataByteBuffer(new Metadata((ObjectNode) json.get("metadata")), byteBuffer))));
            } catch (JsonProcessingException e) {
                sink.error(e);
            }
        });
    }
}
