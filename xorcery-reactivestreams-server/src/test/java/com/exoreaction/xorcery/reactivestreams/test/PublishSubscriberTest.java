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
package com.exoreaction.xorcery.reactivestreams.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreams.cancelStream;

public class PublishSubscriberTest {

    private String clientConf = """
                instance.id: client
                jetty.server.enabled: false
                reactivestreams.server.enabled: false
""";

    private String serverConf = """
                instance.id: server
                jetty.client.enabled: false
                reactivestreams.client.enabled: false
                jetty.server.http.enabled: false
                jetty.server.ssl.port: "{{ SYSTEM.port }}"
""";

    private Configuration clientConfiguration;
    private Configuration serverConfiguration;

    private ReactiveStreamsServerConfiguration reactiveStreamsServerConfiguration;

    @BeforeEach
    public void setup() {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).build();
        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).build();
        reactiveStreamsServerConfiguration = new ReactiveStreamsServerConfiguration(serverConfiguration.getConfiguration("reactivestreams.server"));
    }

    @Test
    public void testPublisherSendResult() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                LogManager.getLogger().info(serverConfiguration);
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);


                CompletableFuture<Integer> result = new CompletableFuture<>();
                CompletableFuture<Void> publisherComplete = reactiveStreamsServer.subscriber("numbers", config -> new IntegerSubscriber(result, new CompletableFuture<>()), IntegerSubscriber.class);

                // When
                ClientPublisher publisher = new ClientPublisher();
                CompletableFuture<Void> stream = reactiveStreamsClient.publish(reactiveStreamsServerConfiguration.getURI(), "numbers",
                        Configuration::empty, publisher, ClientPublisher.class, ClientConfiguration.defaults());

                for (int i = 0; i < 10; i++) {
                    publisher.publish(i);
                }
                publisher.close();

                LogManager.getLogger().info("Closed publisher");

                // Then
                result.orTimeout(10, TimeUnit.SECONDS)
                        .exceptionallyCompose(cancelStream(stream))
                        .whenComplete(this::report)
                        .toCompletableFuture().join();
            }
        }
    }

    @Test
    public void testServerTimesOut() throws Exception {

        serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(serverConf).addYaml("""
                reactivestreams.server.idleTimeout: "2s"
                """).build();

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Integer> result = new CompletableFuture<>();
                CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> new IntegerSubscriber(result, new CompletableFuture<>()), IntegerSubscriber.class);

                // When
                ClientPublisher publisher = new ClientPublisher();
                CompletableFuture<Void> stream = reactiveStreamsClient.publish(reactiveStreamsServerConfiguration.getURI(), "numbers",
                        Configuration::empty, publisher, ClientPublisher.class, ClientConfiguration.defaults());

                for (int i = 0; i < 3; i++) {
                    publisher.publish(i).join();

                    Thread.sleep(3000);
                }
                publisher.close();

                // Then
                result.orTimeout(10, TimeUnit.SECONDS)
                        .exceptionallyCompose(cancelStream(stream))
                        .whenComplete(this::report)
                        .toCompletableFuture().join();
            }
        }
    }

    @Test
    public void testClientTimesOut() throws Exception {

        clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(clientConf).addYaml("""
                reactivestreams.client.idleTimeout: "2s"
                """).build();

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Integer> result = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(result, new CompletableFuture<>());
                CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> subscriber, IntegerSubscriber.class);

                // When
                ClientPublisher publisher = new ClientPublisher();
                CompletableFuture<Void> stream = reactiveStreamsClient.publish(reactiveStreamsServerConfiguration.getURI(), "numbers",
                        Configuration::empty, publisher, ClientPublisher.class, ClientConfiguration.defaults());

                for (int i = 0; i < 3; i++) {
                    publisher.publish(i).join();

                    Thread.sleep(3000);
                }
                publisher.close();
                stream.join();

                // Then
                result.orTimeout(10, TimeUnit.SECONDS)
                        .exceptionallyCompose(cancelStream(stream))
                        .whenComplete(this::report)
                        .toCompletableFuture().join();
            }
        }
    }

    @Test
    public void testSubscriberShutdownWithoutRetryCausesOnError() throws Exception {

        // Given
        CompletableFuture<Void> error = new CompletableFuture<>();
        CompletableFuture<Void> stream = null;

        try (Xorcery client = new Xorcery(clientConfiguration)) {
            try (Xorcery server = new Xorcery(serverConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Integer> result = new CompletableFuture<>();
                CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> new IntegerSubscriber(result, error), IntegerSubscriber.class);

                // When
                ClientPublisher publisher = new ClientPublisher();
                stream = reactiveStreamsClient.publish(reactiveStreamsServerConfiguration.getURI(), "numbers",
                        Configuration::empty, publisher, ClientPublisher.class, new ClientConfiguration.Builder().isRetryEnabled(false).build());
                Thread.sleep(2000);
            }
            Thread.sleep(2000);
        }

        // Then
        Assertions.assertThrows(CompletionException.class, error::join);
        Assertions.assertThrows(CompletionException.class, stream::join);
    }

    @Test
    public void testInactivePublisherCausesReconnect() throws Exception {

        // Given
        try (Xorcery server = new Xorcery(serverConfiguration.asBuilder()
                .add("reactivestreams.server.idleTimeout", "3s")
                .build())) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                ReactiveStreamsServer reactiveStreamsServer = server.getServiceLocator().getService(ReactiveStreamsServer.class);
                ReactiveStreamsClient reactiveStreamsClient = client.getServiceLocator().getService(ReactiveStreamsClient.class);

                CompletableFuture<Integer> subscriberTotal = new CompletableFuture<>();
                IntegerSubscriber subscriber = new IntegerSubscriber(subscriberTotal, new CompletableFuture<>());
                CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> subscriber, IntegerSubscriber.class);

                // When
                IntegerNoopPublisher publisher = new IntegerNoopPublisher();
                CompletableFuture<Void> stream = reactiveStreamsClient.publish(reactiveStreamsServerConfiguration.getURI(), "numbers",
                        Configuration::empty, publisher, IntegerNoopPublisher.class, ClientConfiguration.defaults());

                // Wait
                Thread.sleep(5000);
                publisher.getSubscriber().join().onNext(1);
                Thread.sleep(5000);
                publisher.getSubscriber().join().onNext(1);
                Thread.sleep(5000);
                publisher.getSubscriber().join().onNext(1);
                publisher.getSubscriber().join().onComplete();
                LogManager.getLogger().info("Completed publisher");

                // Then
                Assertions.assertEquals(3, subscriberTotal.join());
            }
        }
    }

    private void report(Object result, Throwable throwable) {
        if (throwable != null)
            LogManager.getLogger().error("Error", throwable);
        else {
            LogManager.getLogger().info("Result:" + result);
        }
    }

    public static class IntegerSubscriber
            implements Flow.Subscriber<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        private int total = 0;
        private Flow.Subscription subscription;
        private final CompletableFuture<Integer> complete;
        private final CompletableFuture<Void> error;

        public IntegerSubscriber(CompletableFuture<Integer> complete, CompletableFuture<Void> error) {
            this.complete = complete;
            this.error = error;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1024);
        }

        @Override
        public void onNext(Integer item) {
            total += item;
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            error.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            logger.info("Subscriber complete, total:" + total);
            complete.complete(total);
        }
    }

    public static class ServerIntegerPublisher
            implements Flow.Publisher<Integer> {

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {

                int current = 0;
                int max = 100;

                @Override
                public void request(long n) {

                    for (long i = 0; i < n && current < max; i++) {
                        subscriber.onNext(current++);
                    }

                    if (current == 100)
                        subscriber.onComplete();
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    public static class IntegerNoopPublisher
            implements Flow.Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        private CompletableFuture<Flow.Subscriber<? super Integer>> subscriber = new CompletableFuture<>();

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            this.subscriber.complete(subscriber);
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {

                }

                @Override
                public void cancel() {
                    logger.info("Cancelled");
                }
            });
        }

        public CompletableFuture<Flow.Subscriber<? super Integer>> getSubscriber() {
            return subscriber;
        }
    }

    public static class ServerIntegerExceptionPublisher
            implements Flow.Publisher<Integer> {

        Logger logger = LogManager.getLogger(getClass());

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            subscriber.onError(new NotAuthorizedException("Not authorized"));
        }
    }


    public static class ClientPublisher
            implements Flow.Publisher<Integer>, AutoCloseable {

        private final CompletableFuture<Void> done = new CompletableFuture<>();
        private final Semaphore semaphore = new Semaphore(0);
        private Flow.Subscriber<? super Integer> subscriber;

        public CompletableFuture<Void> publish(Integer item) {
            try {
                while (!semaphore.tryAcquire(3, TimeUnit.SECONDS)) {
                    // Try again
                    if (done.isDone())
                        return CompletableFuture.failedFuture(new CancellationException());
                }
                subscriber.onNext(item);
                return CompletableFuture.completedFuture(null);
            } catch (InterruptedException e) {
                return CompletableFuture.failedFuture(new CancellationException());
            }
        }

        @Override
        public void close() throws Exception {
            subscriber.onComplete();
        }

        public CompletableFuture<Void> getDone() {
            return done;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    semaphore.release((int) n);
                }

                @Override
                public void cancel() {
                    done.complete(null);
                    semaphore.drainPermits();
                }
            });
        }
    }
}
