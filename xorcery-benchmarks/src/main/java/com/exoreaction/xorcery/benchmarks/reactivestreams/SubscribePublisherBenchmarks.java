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
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.*;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1, jvmArgs = "-Dcom.sun.management.jmxremote=true")
@Warmup(iterations = 1)
@Measurement(iterations = 30)
public class SubscribePublisherBenchmarks {

    private static final String config = """
            secrets.enabled: true
            keystores.enabled: true
            dns.client.enabled: true
                        
            dns.client.hosts:
                server.xorcery.test: "{{ instance.ip }}"
                        
            jetty.client.enabled: true
            jetty.client.ssl.enabled: true
            jetty.client.http2.enabled: true
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

    private ServerPublisher serverPublisher;
    private CompletableFuture<Void> stream;

    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
                .include(SubscribePublisherBenchmarks.class.getSimpleName() + ".*")
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

        ClientSubscriber<WithMetadata<JsonNode>> clientSubscriber = new ClientSubscriber<>() {

            int i = 0;

            @Override
            public void onNext(WithMetadata<JsonNode> item) {
                i++;
                subscription.request(1);
            }
        };
        serverPublisher = new ServerPublisher();

        Configuration serverConf = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(serverConfig).build();
        System.out.println(serverConf);
        server = new Xorcery(serverConf);
        ReactiveStreamsServer reactiveStreams = server.getServiceLocator().getService(ReactiveStreamsServer.class);
        Logger logger = LogManager.getLogger(getClass());
        reactiveStreams.publisher("serverpublisher", cfg -> serverPublisher, serverPublisher.getClass());

        // Client subscriber
        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(clientConfig).build();
        // Use the configuration as payload
//        payload = configuration.json();
        payload = JsonNodeFactory.instance.textNode("foo");

        client = new Xorcery(configuration);

        ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);
        ClientConfiguration clientConfiguration = new ClientConfiguration.Builder()
//                .extensions("permessage-deflate")
                .build();
        stream = reactiveStreamsClient.subscribe(ReactiveStreamsServerConfiguration.get(serverConf).getURI(), "serverpublisher",
                Configuration::empty, clientSubscriber, (Class<? extends Subscriber<?>>) clientSubscriber.getClass(), clientConfiguration);

        logger.info("Setup done");
    }

    @TearDown
    public void teardown() throws Exception {
        stream.complete(null);
        client.close();
        server.close();
        LogManager.getLogger().info("Teardown done");
    }

    private JsonNode payload;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes() throws ExecutionException, InterruptedException, TimeoutException {
        serverPublisher.publish(new WithMetadata<>(new Metadata.Builder()
                .add("foo", "bar")
                .build(), payload));
    }

    public static abstract class ClientSubscriber<T>
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

    public static class ServerPublisher
            implements Publisher<WithMetadata<JsonNode>> {

        private final CompletableFuture<Void> done = new CompletableFuture<>();
        private final Semaphore semaphore = new Semaphore(0);
        private Subscriber<? super WithMetadata<JsonNode>> subscriber;

        public void publish(WithMetadata<JsonNode> item) throws InterruptedException {
            while (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                // Try again
                LogManager.getLogger().warn("Waiting for requests:" + semaphore.availablePermits());
            }
            subscriber.onNext(item);
        }

        @Override
        public void subscribe(Subscriber<? super WithMetadata<JsonNode>> subscriber) {
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
