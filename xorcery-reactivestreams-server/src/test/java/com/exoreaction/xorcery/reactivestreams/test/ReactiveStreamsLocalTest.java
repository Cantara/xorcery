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

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.util.TypeConverterProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreams.cancelStream;

public class ReactiveStreamsLocalTest {

    @Test
    public void testLocalPublishWithTypeConversion() throws Exception {
        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        System.out.println(configuration);

        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.subscriber("numbers", config -> new JsonSubscriber(), JsonSubscriber.class);

            // When
            MessageWorkers messageWorkers = xorcery.getServiceLocator().getService(MessageWorkers.class);
            TypeConverterProcessor<Integer, JsonNode> converter = new TypeConverterProcessor<>(messageWorkers.newWriter(Integer.class, Integer.class, "*/*"), messageWorkers.newReader(JsonNode.class, JsonNode.class, "application/json"));
            IntegerPublisher publisher = new IntegerPublisher();
            publisher.subscribe(converter);
            CompletableFuture<Void> stream = reactiveStreamsClient.publish((URI) null, "numbers",
                    Configuration::empty, converter, IntegerPublisher.class, ClientConfiguration.defaults());

            // Then
            stream.orTimeout(1000, TimeUnit.SECONDS)
                    .toCompletableFuture().join();

            subscriberComplete.complete(null);
        }
    }

    @Test
    public void testLocalSubscribeWithTypeConversion() throws Exception {
        // Given
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        System.out.println(configuration);

        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            CompletableFuture<Void> subscriberComplete = reactiveStreamsServer.publisher("numbers", config -> new IntegerPublisher(), IntegerPublisher.class);

            // When
            MessageWorkers messageWorkers = xorcery.getServiceLocator().getService(MessageWorkers.class);
            TypeConverterProcessor<Integer, JsonNode> converter = new TypeConverterProcessor<>(messageWorkers.newWriter(Integer.class, Integer.class, "*/*"), messageWorkers.newReader(JsonNode.class, JsonNode.class, "application/json"));
            JsonSubscriber subscriber = new JsonSubscriber();
            converter.subscribe(subscriber);
            CompletableFuture<Void> stream = reactiveStreamsClient.subscribe((URI) null, "numbers",
                    Configuration::empty, converter, JsonSubscriber.class, ClientConfiguration.defaults());

            // Then
            stream.orTimeout(1000, TimeUnit.SECONDS)
                    .exceptionallyCompose(cancelStream(stream))
                    .toCompletableFuture().join();
        }
    }

    public static class IntegerPublisher
            implements Flow.Publisher<Integer> {

        private final Logger logger = LogManager.getLogger(getClass());

        public IntegerPublisher() {
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {

                int current = 0;
                int max = 100;

                @Override
                public void request(long n) {

                    for (long i = 0; i < n && current < max; i++) {

                        CompletableFuture<Integer> response = new CompletableFuture<Integer>();
                        response.whenComplete((v, t) ->
                        {
                            logger.info("Result:" + v);
                        });
                        subscriber.onNext(current++);
                    }

                    if (current == 100) {
                        logger.info("Subscriber complete");
                        subscriber.onComplete();
                        current++;
                    }
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    public static class JsonSubscriber
            implements Flow.Subscriber<JsonNode> {

        Logger logger = LogManager.getLogger(getClass());
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(100);
        }

        @Override
        public void onNext(JsonNode item) {
            logger.info("Received integer:" + item);
            subscription.request(1);
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
