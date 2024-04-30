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
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.metadata.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
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
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1, jvmArgs = "-Dcom.sun.management.jmxremote=true")
@Warmup(iterations = 1)
@Measurement(iterations = 3)
public class PublishSubscriberWithResultBenchmarks {

    private static final String config = """
            secrets.enabled: true
            keystores.enabled: true
            dns.client.enabled: true
                        
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
            jetty.server.ssl.enabled: true
            jetty.server.ssl.port: 8443
            reactivestreams.server.enabled: true
            """;

    private ClientPublisher clientPublisher;
    private CompletableFuture<Void> stream;

    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
                .include(PublishSubscriberWithResultBenchmarks.class.getSimpleName() + ".*")
                .forks(0)
                .jvmArgs("-Dcom.sun.management.jmxremote=true")
                .build()).run();
    }

    private Xorcery server;
    private Xorcery client;

    AtomicInteger total = new AtomicInteger();
/*
    ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(10000);
    AtomicInteger writeCount = new AtomicInteger();
*/

    @Setup()
    public void setup() throws Exception {

        Configuration serverConf = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(serverConfig).build();
        System.out.println(serverConf);
        server = new Xorcery(serverConf);
        ReactiveStreamsServer reactiveStreams = server.getServiceLocator().getService(ReactiveStreamsServer.class);
        Logger logger = LogManager.getLogger(getClass());

        // Server subscriber
        ServerSubscriber<WithResult<WithMetadata<String>, Integer>> serverSubscriber = new ServerSubscriber<>() {
            int i = 0;
            final int batchSize = 1024 * 4;

            @Override
            public void onNext(WithResult<WithMetadata<String>, Integer> item) {
                item.result().complete(item.event().data().length());
                i++;
                if (i % batchSize == 0) {
                    subscription.request(batchSize);
                }
            }
        };

        reactiveStreams.subscriber("serversubscriber", cfg -> serverSubscriber, (Class<? extends Subscriber<?>>) serverSubscriber.getClass());

        // Client publisher
        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(clientConfig).build();
        client = new Xorcery(configuration);
        clientPublisher = new ClientPublisher();

        ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);
        stream = reactiveStreamsClient.publish(ReactiveStreamsServerConfiguration.get(configuration).getURI(), "serversubscriber",
                Configuration::empty, clientPublisher, (Class<? extends Publisher<?>>) clientPublisher.getClass(), ClientConfiguration.defaults());

        logger.info("Setup done");
    }

    @TearDown
    public void teardown() throws Exception {
        stream.complete(null);
//        clientPublisher.getDone().get();
        client.close();
        server.close();
        LogManager.getLogger().info("Teardown done");
        LogManager.getLogger().info("Total:" + total.get());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes() throws ExecutionException, InterruptedException, TimeoutException {

        CompletableFuture<Integer> integerCompletableFuture = new CompletableFuture<>();
        integerCompletableFuture.whenComplete((i, t) -> total.addAndGet(i));
        clientPublisher.publish(new WithResult<>(new MetadataByteBuffer(new Metadata.Builder()
                .add("foo", "bar")
                .build(), ByteBuffer.wrap((System.currentTimeMillis() + "").getBytes(StandardCharsets.UTF_8))), integerCompletableFuture));
    }

    public static abstract class ServerSubscriber<T>
            implements Subscriber<T> {

        protected Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(8192);
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {
        }
    }

    public static class ClientPublisher
            implements Publisher<WithResult<MetadataByteBuffer, Integer>> {

        private final CompletableFuture<Void> done = new CompletableFuture<>();
        private final Semaphore semaphore = new Semaphore(0);
        private Subscriber<? super WithResult<MetadataByteBuffer, Integer>> subscriber;

        public void publish(WithResult<MetadataByteBuffer, Integer> item) throws InterruptedException {
            if (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                // Try again
                publish(item);
            }
            subscriber.onNext(item);
        }

        public CompletableFuture<Void> getDone() {
            return done;
        }

        @Override
        public void subscribe(Subscriber<? super WithResult<MetadataByteBuffer, Integer>> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    semaphore.release((int) n);
//                    LogManager.getLogger().warn("Current requests:" + semaphore.availablePermits());
                }

                @Override
                public void cancel() {
                    done.complete(null);
                }
            });
        }
    }
}
