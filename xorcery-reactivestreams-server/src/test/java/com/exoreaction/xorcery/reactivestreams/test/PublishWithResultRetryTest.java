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
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PublishWithResultRetryTest {

    Logger logger = LogManager.getLogger();

    @Test
    public void testServerTimesOut() throws Exception {

        // Given
        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(String.format("""
                reactivestreams.server.idleTimeout: "5s"
                jetty.server.http.port: %d
                jetty.server.ssl.port: %d
                """, Sockets.nextFreePort(), Sockets.nextFreePort())).build();
        logger.debug(configuration);

        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> new ServerIntegerWithDelaySubscriber(), ServerIntegerWithDelaySubscriber.class);

            // When
            ClientIntegerWithResultPublisher publisher = new ClientIntegerWithResultPublisher();
            CompletableFuture<Void> publisherResult = reactiveStreamsClient.publish(ReactiveStreamsServerConfiguration.get(configuration).getURI(), "numbers",
                    () -> new ConfigurationBuilder().addYaml("""
                            foo: bar
                            test: 42
                            """).build(), publisher, ClientIntegerWithResultPublisher.class, ClientConfiguration.defaults());


            // Then
//            Assertions.assertThrows(CompletionException.class, () ->
//            {
            int resultInt = -1;
            while (resultInt == -1) {
                try {
                    resultInt = publisher.publish(42).orTimeout(30, TimeUnit.SECONDS).join();
                    logger.debug("Result:" + resultInt);
                } catch (Throwable e) {
                    logger.debug("Error", e);
                }
            }
//            });

            logger.debug("Done");
            publisher.close();
            publisherResult.join();

            Thread.sleep(5000);
        }
    }

    public static class ClientIntegerWithResultPublisher
            extends ClientWithResultPublisher<Integer, Integer>
            implements Publisher<WithResult<Integer, Integer>> {
    }

    public static class ClientWithResultPublisher<T, R>
            implements AutoCloseable {

        private Subscriber<? super WithResult<T, R>> subscriber;
        private final Semaphore requested = new Semaphore(0);

        public ClientWithResultPublisher() {
        }

        public void subscribe(Subscriber<? super WithResult<T, R>> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Subscription() {

                @Override
                public void request(long n) {

                    requested.release((int) n);
                }

                @Override
                public void cancel() {
                }
            });
        }

        public CompletableFuture<R> publish(T item) {
            CompletableFuture<R> response = new CompletableFuture<R>();
            try {
                requested.acquire();
                subscriber.onNext(new WithResult<>(item, response));
                return response;
            } catch (InterruptedException e) {
                response.completeExceptionally(e);
                return response;
            }
        }

        @Override
        public void close() throws Exception {
            subscriber.onComplete();
        }
    }

    public static class ServerIntegerWithDelaySubscriber
            implements Subscriber<WithResult<Integer, Integer>> {

        Logger logger = LogManager.getLogger(getClass());
        private static int delay = 7;
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(100);
        }

        @Override
        public void onNext(WithResult<Integer, Integer> item) {
            logger.info("Received integer:" + item.event());
            int currentDelay = Math.max(delay--, 0);
            CompletableFuture.delayedExecutor(currentDelay, TimeUnit.SECONDS).execute(() ->
            {
                logger.info("Calculated integer:" + item.event());
                item.result().complete(item.event());
                subscription.request(1);
            });
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error("Subscriber error", throwable);
        }

        @Override
        public void onComplete() {
            logger.info("Subscriber onComplete");
        }
    }
}
