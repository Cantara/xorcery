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
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1, jvmArgs = "-Dcom.sun.management.jmxremote=true")
@Warmup(iterations = 1)
@Measurement(iterations = 30)
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

    private ClientPublisher clientPublisher;
    private Disposable clientDisposable;
    private Disposable serverDisposable;

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

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LogManager.getLogger().error("Uncaught exception " + t.getName(), e);
            }
        });

        Configuration serverConf = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(serverConfig).build();
        System.out.println(serverConf);
        server = new Xorcery(serverConf);
        WebSocketStreamsServer server = this.server.getServiceLocator().getService(WebSocketStreamsServer.class);
        Logger logger = LogManager.getLogger(getClass());

        serverDisposable = server.subscriber("serversubscriber", MediaType.APPLICATION_JSON, ByteBuffer.class, flux -> flux);

        // Client publisher
        Configuration clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(clientConfig).build();
        byteBufferPayload = ByteBuffer.wrap("\"foo\"".getBytes(StandardCharsets.UTF_8));

        client = new Xorcery(clientConfiguration);
        clientPublisher = new ClientPublisher();

        WebSocketStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(WebSocketStreamsClient.class);
        clientDisposable = Flux.from(reactiveStreamsClient.publish(
                ReactiveStreamsServerConfiguration.get(serverConf).getURI().resolve("serversubscriber"),
                MediaType.APPLICATION_JSON,
                ByteBuffer.class,
                WebSocketClientOptions.empty(),
                clientPublisher)).subscribe();

        logger.info("Setup done");
    }

    @TearDown
    public void teardown() throws Exception {
        clientDisposable.dispose();
        serverDisposable.dispose();
        client.close();
        server.close();
        LogManager.getLogger().info("Teardown done");
    }

    private ByteBuffer byteBufferPayload;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes() throws InterruptedException {
        clientPublisher.publish(byteBufferPayload);
    }

    public static abstract class ServerSubscriber<T>
            extends BaseSubscriber<T> {
    }

    public static class ClientPublisher
            implements Publisher<ByteBuffer> {

        private final Semaphore semaphore = new Semaphore(0);
        private Subscriber<? super ByteBuffer> subscriber;

        public void publish(ByteBuffer item) throws InterruptedException {
            while (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                // Try again
                LogManager.getLogger().warn("Waiting for requests:" + semaphore.availablePermits());
            }
            subscriber.onNext(item);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    semaphore.release((int) n);
                }

                @Override
                public void cancel() {
                }
            });
        }
    }
}
