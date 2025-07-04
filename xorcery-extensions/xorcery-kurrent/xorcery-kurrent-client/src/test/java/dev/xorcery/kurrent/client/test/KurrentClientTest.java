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
package dev.xorcery.kurrent.client.test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.kurrent.client.api.KurrentClient;
import dev.xorcery.kurrent.client.api.KurrentClients;
import dev.xorcery.kurrent.client.api.KurrentContext;
import dev.xorcery.kurrent.client.api.KurrentMetadata;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.ReactiveStreamsContext;
import dev.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import dev.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import dev.xorcery.reactivestreams.util.ReactiveStreams;
import dev.xorcery.util.Resources;
import io.kurrent.dbclient.DeleteResult;
import io.kurrent.dbclient.StreamAcl;
import io.kurrent.dbclient.StreamMetadata;
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
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unchecked")
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(60)
public class KurrentClientTest {

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yaml"))
                    .withLogConsumer("kurrent", new Slf4jLogConsumer(LoggerFactory.getLogger(KurrentClientTest.class)))
                    .withExposedService("kurrent", 2115, Wait.forListeningPorts(2115))
                    .withStartupTimeout(Duration.ofMinutes(10));

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    private static KurrentClient client;

    @BeforeAll
    public static void setup(KurrentClients clients) {
        client = clients.getDefaultClient();
    }

    @Test
    @Order(1)
    public void append() {
        Flux<MetadataByteBuffer> appendFlux = Flux
                .from(new YamlPublisher<ObjectNode>(ObjectNode.class))
                .handle(ReactiveStreams.toMetadataByteBuffer("metadata", "data"))
                .transformDeferredContextual(client.appendStream(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null));

        {
            URL yamlResource = Resources.getResource("testevents1.yaml").orElseThrow();
            List<MetadataByteBuffer> result = appendFlux
                    .contextWrite(Context.of(
                            ResourcePublisherContext.resourceUrl, yamlResource.toExternalForm(),
                            ReactiveStreamsContext.streamId, "test-eventstream"
                    ))
                    .timeout(Duration.ofSeconds(30))
                    .toStream()
                    .toList();

//            System.out.println(result);
            Assertions.assertEquals(3, result.size());
        }
    }

    @Test
    @Order(2)
    public void resumeFromLastPosition() {
        Flux<MetadataByteBuffer> resumeFlux = Flux
                .from(new YamlPublisher<ObjectNode>(ObjectNode.class))
                .handle(ReactiveStreams.toMetadataByteBuffer("metadata", "data"))
                .transformDeferredContextual(client.appendStream(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null));

        {
            URL yamlResource = Resources.getResource("testevents2.yaml").orElseThrow();
            List<MetadataByteBuffer> result = resumeFlux
                    .contextWrite(Context.of(
                            ResourcePublisherContext.resourceUrl, yamlResource.toExternalForm(),
                            ReactiveStreamsContext.streamId, "test-eventstream"
                    ))
                    .timeout(Duration.ofSeconds(30))
                    .toStream()
                    .toList();

//            System.out.println(result);
            Assertions.assertEquals(8, result.size());
        }
    }

    @Test
    @Order(3)
    public void readStream() {

        Flux<MetadataByteBuffer> readFlux = Flux.from(client.readStream());
        {
            List<MetadataByteBuffer> result = readFlux
                    .contextWrite(Context.of(ReactiveStreamsContext.streamId, "test-eventstream"))
                    .timeout(Duration.ofSeconds(30))
                    .toStream()
                    .toList();
//            System.out.println(result);
            Assertions.assertEquals(11, result.size());
        }
    }

    @Test
    @Order(4)
    public void readStreamSubscription() throws InterruptedException {

        Flux<MetadataByteBuffer> readSubscriptionFlux = Flux.from(client.readStream());
        {
            AtomicInteger counter = new AtomicInteger();
            List<MetadataByteBuffer> result = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            Disposable disposable = readSubscriptionFlux
                    .<MetadataByteBuffer>handle((val, sink) ->
                    {
                        val.metadata().getString("timestamp").ifPresent(System.out::println);
                        if (counter.incrementAndGet() >= 11) {
                            sink.complete();
                            latch.countDown();
                            System.out.println("DONE!");
                        } else
                            sink.next(val);
                    })
                    .doOnNext(result::add)
                    .doOnError(System.out::println)
                    .contextWrite(Context.of(ReactiveStreamsContext.streamId, "test-eventstream",
                            KurrentContext.keepAlive, true))
                    .timeout(Duration.ofSeconds(30))
                    .subscribe();
            latch.await(10, TimeUnit.SECONDS);
            disposable.dispose();

//            System.out.println(result);
            Assertions.assertEquals(10, result.size());
        }
    }

    @Test
    @Order(5)
    public void appendAndRead() throws InterruptedException {
        Flux<MetadataByteBuffer> appendFlux = Flux
                .from(new YamlPublisher<ObjectNode>(ObjectNode.class))
                .handle(ReactiveStreams.toMetadataByteBuffer("metadata", "data"))
                .transformDeferredContextual(client.appendStream(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null));

        Flux<MetadataByteBuffer> readSubscriptionFlux = Flux.from(client.readStream());

        // Append events from file to a streamId
        URL yamlResource = Resources.getResource("testevents2.yaml").orElseThrow();
        Disposable appender = appendFlux
                .contextWrite(Context.of(
                        ResourcePublisherContext.resourceUrl, yamlResource.toExternalForm(),
                        ReactiveStreamsContext.streamId, "test-eventstream"
                ))
                .timeout(Duration.ofSeconds(30))
                .subscribe();

        // Read events from projection
        AtomicInteger counter = new AtomicInteger();
        List<String> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        readSubscriptionFlux
                .map(val -> val.metadata().getString("timestamp").get())
                .doOnNext(result::add)
                .<String>handle((val, sink) ->
                {
                    if (counter.incrementAndGet() == 11) {
                        sink.complete();
                        latch.countDown();
                        System.out.println("DONE!");
                    } else
                        sink.next(val);
                })
                .doOnError(System.out::println)
                .contextWrite(Context.of(
                        ReactiveStreamsContext.streamId, "$ce-test",
                        KurrentContext.keepAlive, true
                ))
                .timeout(Duration.ofSeconds(30))
                .subscribe();
        latch.await(5, TimeUnit.SECONDS);

        appender.dispose();

//        System.out.println(result);
        Assertions.assertEquals(11, result.size());
    }

    @Test
    @Order(6)
    public void idempotentAppend() {
        UUID fixedId = UUID.randomUUID();

        // We have to do this because otherwise YamlPublisher will use the resume-from-last-position and nothing will happen
        URL yamlResource = Resources.getResource("testevents2.yaml").orElseThrow();
        List<ObjectNode> events = Flux.from(new YamlPublisher<ObjectNode>(ObjectNode.class))
                .skipLast(10)
                .contextWrite(Context.of(
                        ResourcePublisherContext.resourceUrl, yamlResource.toExternalForm()
                ))
                .timeout(Duration.ofSeconds(30))
                .toStream().toList();

        Flux<MetadataByteBuffer> appendFlux = Flux.fromIterable(events)
                .handle(ReactiveStreams.toMetadataByteBuffer("metadata", "data"))
                .transformDeferredContextual(client.appendStream(
                        metadata -> fixedId,
                        metadata -> "DomainCommand",
                        null))
                .contextWrite(Context.of(
                        ReactiveStreamsContext.streamId, "test-eventstreamidempotent"
                )).timeout(Duration.ofSeconds(30));

        {
            List<Long> result = appendFlux
                    .toStream()
                    .map(mdbb -> mdbb.metadata().getLong(ReactiveStreamsContext.streamPosition).orElseThrow())
                    .toList();

            // Write same event id again
            List<Long> result2 = appendFlux
                    .toStream()
                    .map(mdbb -> mdbb.metadata().getLong(ReactiveStreamsContext.streamPosition).orElseThrow())
                    .toList();

//            System.out.println(result2);

            Assertions.assertEquals(result, result2);
        }
    }

    @Test
    public void appendWithOptimisticLocking() {

        // Make first commit
        MetadataByteBuffer commit = Mono.just(new MetadataByteBuffer(new Metadata.Builder().build(), ByteBuffer.wrap("eventdata".getBytes(StandardCharsets.UTF_8))))
                .handle(client.appendOptimisticLocking(metadata -> UUID.randomUUID(), metadata -> "DomainCommand", null))
                .contextWrite(Context.of(ReactiveStreamsContext.streamId, "domainevents-1234"))
                .toFuture().orTimeout(10, TimeUnit.SECONDS).join();

        // Make second commit, and now we know what position to expect in Kurrent
        MetadataByteBuffer commit2 = Mono.just(new MetadataByteBuffer(new Metadata.Builder().build(), ByteBuffer.wrap("eventdata".getBytes(StandardCharsets.UTF_8))))
                .doOnNext(mdbb -> mdbb.metadata().toBuilder().add(KurrentMetadata.expectedPosition, commit.metadata().getLong(ReactiveStreamsContext.streamPosition).orElseThrow()))
                .handle(client.appendOptimisticLocking(metadata -> UUID.randomUUID(), metadata -> "DomainCommand", null))
                .contextWrite(Context.of(
                        ReactiveStreamsContext.streamId, "domainevents-1234"
                ))
                .toFuture().orTimeout(10, TimeUnit.SECONDS).join();

        // Make third commit against same position, should fail
        Assertions.assertThrows(CompletionException.class, () ->
        {
            MetadataByteBuffer commit3 = Mono.just(new MetadataByteBuffer(new Metadata.Builder().build(), ByteBuffer.wrap("eventdata".getBytes(StandardCharsets.UTF_8))))
                    .doOnNext(mdbb -> mdbb.metadata().toBuilder().add(KurrentMetadata.expectedPosition, commit.metadata().getLong(ReactiveStreamsContext.streamPosition).orElseThrow()))
                    .handle(client.appendOptimisticLocking(metadata -> UUID.randomUUID(), metadata -> "DomainCommand", null))
                    .contextWrite(Context.of(
                            ReactiveStreamsContext.streamId, "domainevents-1234"
                    ))
                    .toFuture().orTimeout(10, TimeUnit.SECONDS).join();
        });
    }

    @Test
    @Disabled("randomly fails in CI, needs investigation")
    public void deleteProjectedStream() throws InterruptedException {
        Flux<MetadataByteBuffer> appendFlux = Flux.from(new YamlPublisher<ObjectNode>(ObjectNode.class))
                .handle(ReactiveStreams.toMetadataByteBuffer("metadata", "data"))
                .transformDeferredContextual(client.appendStream(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null));

        Flux<MetadataByteBuffer> readStreamFlux = Flux.from(client.readStream());

        URL yamlResource = Resources.getResource("testevents1.yaml").orElseThrow();

        System.out.println("Append 1");
        appendFlux
                .contextWrite(Context.of(
                        ResourcePublisherContext.resourceUrl, yamlResource.toExternalForm(),
                        ReactiveStreamsContext.streamId, "testdelete-eventstream"
                ))
                .blockLast();

        System.out.println("Append 2");
        appendFlux
                .contextWrite(Context.of(
                        ResourcePublisherContext.resourceUrl, yamlResource.toExternalForm(),
                        ReactiveStreamsContext.streamId, "testdelete-eventstream2-foo"
                ))
                .blockLast();

        Thread.sleep(3000); // Just to give the projection some time to catch up

        System.out.println("Read 1");
        List<MetadataByteBuffer> before = readStreamFlux
                .contextWrite(Context.of(ReactiveStreamsContext.streamId, "$ce-testdelete"))
                .toStream()
                .toList();
        Assertions.assertEquals(6, before.size());
//        System.out.println(before);

        System.out.println("Delete ");
        readStreamFlux
                .handle((value, sink) ->
                {
                    value.metadata().getString(KurrentMetadata.originalStreamId).ifPresent(streamId ->
                    {
                        if (streamId.startsWith("testdelete-eventstream2")) {
                            System.out.println("Delete " + streamId);
                            DeleteResult deleteResult = client.getClient().deleteStream(streamId).orTimeout(10, TimeUnit.SECONDS).join();
                        }
                    });
                })
                .contextWrite(Context.of(ReactiveStreamsContext.streamId, "$streams"))
                .blockLast();

        Thread.sleep(3000); // Just to give the projection some time to catch up

        System.out.println("Read 2");
        List<MetadataByteBuffer> after = readStreamFlux
                .contextWrite(Context.of(ReactiveStreamsContext.streamId, "$ce-testdelete"))
                .toStream()
                .toList();
//        System.out.println(after);
        Assertions.assertEquals(3, after.size());

        System.out.println("Streams");
        List<MetadataByteBuffer> streams = readStreamFlux
                .filter(value -> value.metadata().getString(KurrentMetadata.originalStreamId).map(id -> id.startsWith("testdelete")).orElse(false))
                .contextWrite(Context.of(ReactiveStreamsContext.streamId, "$streams"))
                .toStream()
                .toList();
//        System.out.println(streams);
        Assertions.assertEquals(1, streams.size());
    }

    @Test
    public void appendWithStreamMetadata() {
        Flux<MetadataByteBuffer> appendFlux = Flux
                .from(new YamlPublisher<ObjectNode>(ObjectNode.class))
                .handle(ReactiveStreams.toMetadataByteBuffer("metadata", "data"))
                .transformDeferredContextual(client.appendStream(
                        metadata -> UUID.randomUUID(),
                        metadata -> "DomainCommand",
                        null));

        {
            URL yamlResource = Resources.getResource("testevents1.yaml").orElseThrow();
            List<MetadataByteBuffer> result = appendFlux
                    .contextWrite(Context.of(Map.of(
                            ResourcePublisherContext.resourceUrl, yamlResource.toExternalForm(),
                            ReactiveStreamsContext.streamId, "test-metadata",
                            "maxAge", Duration.ofDays(7).toMillis(),
                            "maxCount", 10000,
                            "cacheControl", 10000,
                            "truncateBefore", 2,
                            "acl", """
                                    {"$r":["foo","bar"]}
                                    """,
                            "customProperties", """
                                    {"foo":123}
                                    """
                    )))
                    .timeout(Duration.ofSeconds(30))
                    .toStream()
                    .toList();

            Assertions.assertEquals(3, result.size());

            StreamMetadata streamMetadata = client.getStreamMetadata("test-metadata")
                    .orTimeout(10, TimeUnit.SECONDS).join();

            Assertions.assertEquals(604800000L, streamMetadata.getMaxAge());
            Assertions.assertEquals(10000L, streamMetadata.getMaxCount());
            if (streamMetadata.getAcl() instanceof StreamAcl acl) {
                Assertions.assertEquals(List.of("foo", "bar"), acl.getReadRoles());
            }
            Map<String, Object> customProperties = streamMetadata.getCustomProperties();
            customProperties.remove("resourceUrl");
            Assertions.assertEquals(Map.of("foo", 123, "streamId", "test-metadata"), customProperties);
        }
    }
}
