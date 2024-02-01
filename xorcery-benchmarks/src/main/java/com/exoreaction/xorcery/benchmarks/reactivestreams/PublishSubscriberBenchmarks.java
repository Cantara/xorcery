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
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1, jvmArgs = "-Dcom.sun.management.jmxremote=true")
@Warmup(iterations = 1)
@Measurement(iterations = 30)
public class PublishSubscriberBenchmarks {

    private static final String config = """
            secrets.enabled: true
            keystores.enabled: true
            dns.client.enabled: true
                        
            dns.client.hosts:
                server.xorcery.test: "{{ instance.ip }}"
                        
            jetty.client.enabled: true
            jetty.client.ssl.enabled: true
            reactivestreams.client.enabled: true

            metrics.enabled: false
            metrics.filter:
                - xorcery:*
            metrics.subscriber.authority: localhost:443    
            """;

    private static final String clientConfig = """
            instance.id: client
            """;

    private static final String serverConfig = """
            instance.id: server
            jetty.server.enabled: true
            jetty.server.http.enabled: true
            jetty.server.http2.enabled: true
            jetty.server.ssl.enabled: false
            jetty.server.ssl.port: 8443
            reactivestreams.server.enabled: true
            """;

    private ClientPublisher clientPublisher;
    private CompletableFuture<Void> stream;

    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
                .include(PublishSubscriberBenchmarks.class.getSimpleName() + ".*")
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
                LogManager.getLogger().error("Uncaught exception "+t.getName(), e);
            }
        });

        Configuration serverConf = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(serverConfig).build();
        System.out.println(serverConf);
        server = new Xorcery(serverConf);
        ReactiveStreamsServer reactiveStreams = server.getServiceLocator().getService(ReactiveStreamsServer.class);
        Logger logger = LogManager.getLogger(getClass());

        // Server subscriber
        ServerSubscriber<ByteBuffer> serverSubscriber = new ServerSubscriber<>() {

            int i = 0;

            @Override
            public void onNext(ByteBuffer item) {
                i++;
                subscription.request(1);
            }
        };

        reactiveStreams.subscriber("serversubscriber", cfg -> serverSubscriber, (Class<? extends Subscriber<?>>) serverSubscriber.getClass());

        // Client publisher
        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(clientConfig).build();
        byteBufferPayload = ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8));

        client = new Xorcery(configuration);
        clientPublisher = new ClientPublisher();

        ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);
        ClientConfiguration clientConfiguration = new ClientConfiguration.Builder()
//                .extensions("permessage-deflate")
                .build();
        stream = reactiveStreamsClient.publish(ReactiveStreamsServerConfiguration.get(serverConf).getURI(), "serversubscriber",
                Configuration::empty, clientPublisher, (Class<? extends Publisher<?>>) clientPublisher.getClass(), clientConfiguration);

        logger.info("Setup done");
    }

    @TearDown
    public void teardown() throws Exception {
        stream.complete(null);
//        clientPublisher.getDone().get();
        client.close();
        server.close();
        LogManager.getLogger().info("Teardown done");
    }

    Metadata metadata = new Metadata.Builder()
            .add("foo", "bar")
            .build();
    private ByteBuffer byteBufferPayload;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes() throws ExecutionException, InterruptedException, TimeoutException {
        clientPublisher.publish(byteBufferPayload);
    }

    public static abstract class ServerSubscriber<T>
            implements Subscriber<T> {

        protected Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1024);
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {

        }
    }

    public static class ClientPublisher
            implements Publisher<java.nio.ByteBuffer> {

        private final CompletableFuture<Void> done = new CompletableFuture<>();
        private final Semaphore semaphore = new Semaphore(0);
        private Subscriber<? super ByteBuffer> subscriber;

        public void publish(ByteBuffer item) throws InterruptedException {
            while (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                // Try again
                LogManager.getLogger().warn("Waiting for requests:" + semaphore.availablePermits());
            }
            subscriber.onNext(item);
        }

        public CompletableFuture<Void> getDone() {
            return done;
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
                    done.complete(null);
                }
            });
        }
    }
}
