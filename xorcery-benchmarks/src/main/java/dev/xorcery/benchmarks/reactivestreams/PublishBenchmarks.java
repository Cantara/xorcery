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
package dev.xorcery.benchmarks.reactivestreams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import dev.xorcery.reactivestreams.server.ServerWebSocketStreamsConfiguration;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1, jvmArgs = "-Dcom.sun.management.jmxremote=true")
@Warmup(iterations = 0)
@Measurement(iterations = 3)
public class PublishBenchmarks {

    private static final String config = """
            secrets.enabled: true
            keystores.enabled: true
            dns.client.enabled: true
            
            dns.client.hosts:
                - name: server.xorcery.test
                  url: "{{ instance.ip }}"
            
            jetty.client.enabled: true
            jetty.client.ssl.enabled: true

            reactivestreams.enabled: true

            log4j2.Configuration.thresholdFilter: debug
            
            reactivestreams.client.maxFrameSize: 1048576
            """;

    private static final String clientConfig = """
            instance.id: client
            reactivestreams.server.enabled: false

            log4j2.Configuration.thresholdFilter: debug
            
            log4j2.Configuration.Loggers.logger:
                - name: dev.xorcery.reactivestreams
                  level: info

                - name: org.eclipse.jetty.websocket.core.internal.FrameFlusher
                  level: info
            """;

    private static final String serverConfig = """
            instance.id: server
            jetty.server.enabled: true
            jetty.server.http.enabled: true
            jetty.server.http2.enabled: false
            jetty.server.ssl.enabled: false
            jetty.server.ssl.port: 8443

            reactivestreams.client.enabled: false
            """;

    private Disposable serverDisposable;
    private Configuration serverConf;

    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
                .include(PublishBenchmarks.class.getSimpleName() + ".publishByteBuffer")
                .forks(0)
                .jvmArgs("-Dcom.sun.management.jmxremote=true")
                .build()).run();
    }

    private Xorcery server;
    private Xorcery client;

    @Setup()
    public void setup() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LogManager.getLogger().error("Uncaught exception " + t.getName(), e);
            }
        });

        serverConf = new ConfigurationBuilder()
                .addTestDefaults().addYaml(config).addYaml(serverConfig).build();
        System.out.println(serverConf);
        server = new Xorcery(serverConf);

        Configuration clientConfiguration = new ConfigurationBuilder()
                .addTestDefaults().addYaml(config).addYaml(clientConfig).build();
        client = new Xorcery(clientConfiguration);

        logger.info("Setup done");
    }

    @TearDown
    public void teardown() throws Exception {
        System.out.println("Completed");
        serverDisposable.dispose();
        client.close();
        server.close();
        LogManager.getLogger().info("Teardown done");
    }

    @State(Scope.Thread)
    @AuxCounters(AuxCounters.Type.OPERATIONS)
    public static class OpCounters {
        public int counter;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void publishMetadataByteBuffer(OpCounters counters) throws JsonProcessingException {
        String metadata = """
                  tenant: "acme-small"
                  aggregateId: "acme-small"
                  commandName: "DoStuff"
                  environment: "development"
                  source: "myapp/1.0.0"
                  streamId: "$ce-development"
                  streamPosition: 8836
                  originalStreamId: "events-development-myapp-acme-small"
                """;

        ObjectNode metadataJson = (ObjectNode) new YAMLMapper().readTree(metadata);
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[1024]);
        MetadataByteBuffer metadataByteBuffer = new MetadataByteBuffer(new Metadata(metadataJson), byteBuffer);
        runBenchmark(counters, MetadataByteBuffer.class, Flux.fromStream(Stream.generate(()-> metadataByteBuffer).limit(5000000)), MediaType.APPLICATION_JSON+"metadata");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void publishByteBuffer(OpCounters counters) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4]);
        runBenchmark(counters, ByteBuffer.class, Flux.fromStream(Stream.generate(()-> byteBuffer).limit(10000000)), MediaType.APPLICATION_JSON);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void publishInteger(OpCounters counters) {
        runBenchmark(counters, Integer.class, Flux.fromStream(IntStream.range(0, 10000000).boxed()), MediaType.APPLICATION_JSON);
    }

    private <T> void runBenchmark(OpCounters counters, Class<T> publishType, Flux<T> source, String contentType)
    {
        counters.counter = 0;
        ServerWebSocketStreams streamsServer = this.server.getServiceLocator().getService(ServerWebSocketStreams.class);
        serverDisposable = streamsServer.subscriber("benchmark", ServerWebSocketOptions.instance(), publishType,
                flux -> flux
                        .publishOn(Schedulers.boundedElastic(), 2048)
                        .doOnNext(item ->
                        {
                            counters.counter++;
                        })
                        .doOnComplete(() ->
                        {
                            System.out.println("Invoke count:" + counters.counter);
                        }).subscribe());

        URI serverUri = ServerWebSocketStreamsConfiguration.get(serverConf).getURI().resolve("benchmark");
        System.out.println("Server URI:"+serverUri);
        ClientWebSocketStreams reactiveStreamsClient = client.getServiceLocator().getService(ClientWebSocketStreams.class);

        System.out.println("Start");
        AtomicInteger count = new AtomicInteger();
        CompletableFuture<Void> result = reactiveStreamsClient.publish(
                source.doOnNext(item -> count.incrementAndGet()),
                serverUri,
                ClientWebSocketOptions.builder().extensionPerMessageDeflate().build(),
                publishType, contentType);
        result.join();
        System.out.println("Stop "+count);
        serverDisposable.dispose();
    }
}
