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
package com.exoreaction.xorcery.benchmarks.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1, jvmArgs = "-Dcom.sun.management.jmxremote=true")
@Warmup(iterations = 0)
@Measurement(iterations = 3)
public class PublishSubscriberWebSocketBenchmarks {

    private static final String config = """
            secrets.enabled: true
            keystores.enabled: true
            dns.client.enabled: true
                        
            dns.client.hosts:
                server.xorcery.test: "{{ instance.ip }}"
                        
            jetty.client.enabled: true
            jetty.client.ssl.enabled: true

            reactivestreams.enabled: true

            metrics.enabled: false
            metrics.filter:
                - xorcery:*
            metrics.subscriber.authority: localhost:443    
            """;

    private static final String clientConfig = """
            instance.id: client
            reactivestreams.server.enabled: false
            """;

    private static final String serverConfig = """
            instance.id: server
            jetty.server.enabled: true
            jetty.server.http.enabled: true
            jetty.server.http2.enabled: false
            jetty.server.ssl.enabled: true
            jetty.server.ssl.port: 8443

            reactivestreams.client.enabled: false
            """;

    private Disposable clientDisposable;
    private Disposable serverDisposable;
    private CompletableFuture<ByteBuffer> done;
    private Configuration serverConf;

    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
                .include(PublishSubscriberWebSocketBenchmarks.class.getSimpleName() + ".*")
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
        done.orTimeout(10, TimeUnit.SECONDS).join();
        System.out.println("Completed");
        serverDisposable.dispose();
        client.close();
        server.close();
        LogManager.getLogger().info("Teardown done");
    }

/*
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes() {
        while(clientSink.tryEmitNext(byteBufferPayload).isFailure())
        {};
    }
*/

    @State(Scope.Thread)
    @AuxCounters(AuxCounters.Type.OPERATIONS)
    public static class OpCounters {
        public int counter;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes(OpCounters counters) {
        counters.counter = 0;
        WebSocketStreamsServer streamsServer = this.server.getServiceLocator().getService(WebSocketStreamsServer.class);
        done = new CompletableFuture<>();
        serverDisposable = streamsServer.subscriber("benchmark", Integer.class,
                flux -> flux
                        .doOnNext(item -> counters.counter++)
                        .doOnComplete(() ->
                        {
                            System.out.println("Invoke count:" + counters.counter);
                            done.complete(null);
                        }));

        URI serverUri = ReactiveStreamsServerConfiguration.get(serverConf).getURI().resolve("benchmark");
        WebSocketStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);
        clientDisposable = Flux.fromStream(IntStream.range(0, 1000000).boxed())
                .transform(reactiveStreamsClient.publish(WebSocketClientOptions.instance(), Integer.class, MediaType.APPLICATION_JSON))
                .contextWrite(Context.of(WebSocketStreamContext.serverUri.name(), serverUri))
                .subscribe();

        done.orTimeout(100, TimeUnit.SECONDS).join();
        clientDisposable.dispose();
        serverDisposable.dispose();
    }
}
