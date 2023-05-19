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
package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.*;
import com.exoreaction.xorcery.util.Sockets;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

//@Disabled
public class ClientPublisherServerSubscriberIT {

    private static final String config = """
            keystores.enabled: true
            reactivestreams.enabled: true
            jetty.server.ssl.enabled: true
            jetty.server.http2.enabled: true
            jetty.server.security.enabled: true
            jetty.client.ssl.enabled: true
            """;

    @Test
    public void givenSubscriberWhenPublishEventsThenSubscriberConsumesEvents2() throws Exception {
        //System.setProperty("javax.net.debug", "ssl,handshake");
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();

        System.out.println(StandardConfigurationBuilder.toYaml(configuration));

        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            // Server subscriber
            reactiveStreamsServer.subscriber("serversubscriber", cfg -> new StringServerSubscriber(), StringServerSubscriber.class);

            // Client publisher
            final long total = 1000;
            long start = System.currentTimeMillis();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            int clients = 1;
            for (int i = 0; i < clients; i++) {
                final StringClientPublisher clientPublisher = new StringClientPublisher((int) total);
                InstanceConfiguration standardConfiguration = new InstanceConfiguration(xorcery.getServiceLocator().getService(Configuration.class).getConfiguration("instance"));
                URI serverUri = standardConfiguration.getURI();
                futures.add(reactiveStreamsClient.publish(serverUri.getAuthority(), "serversubscriber",
                        Configuration::empty, clientPublisher, clientPublisher.getClass(), ClientConfiguration.defaults()));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            long end = System.currentTimeMillis();
            long time = end - start;
            System.out.printf("Time: %d, Rate:%d\n", time, (clients * total) / time);
        }
    }

    @Test
    public void givenSubscriberWhenPublishEventsWithResultsThenSubscriberCalculatesResults() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            // Server subscriber
            ServerSubscriber<WithResult<String, Integer>> serverSubscriber = new ServerSubscriber<>() {
                @Override
                public void onNext(WithResult<String, Integer> item) {
                    item.result().complete(item.event().length());
                    subscription.request(1);
                }
            };

            reactiveStreamsServer.subscriber("serversubscriber", cfg -> serverSubscriber, (Class<? extends Flow.Subscriber<?>>) serverSubscriber.getClass());

            // Client publisher
            final ClientPublisher<WithResult<String, Integer>> clientPublisher = new ClientPublisher<>() {
                @Override
                protected void publish(Flow.Subscriber<? super WithResult<String, Integer>> subscriber) {
                    for (int i = 0; i < 100; i++) {
                        CompletableFuture<Integer> future = new CompletableFuture<>();
                        future.whenComplete((r, t) ->
                                System.out.println("Length:" + r));
                        subscriber.onNext(new WithResult<>(i + "", future));
                    }
                    subscriber.onComplete();
                    System.out.println("Completed publishing");
                }
            };

            InstanceConfiguration standardConfiguration = new InstanceConfiguration(xorcery.getServiceLocator().getService(Configuration.class).getConfiguration("instance"));
            URI serverUri = standardConfiguration.getURI();
            reactiveStreamsClient.publish(serverUri.getAuthority(), "serversubscriber", Configuration::empty, clientPublisher, (Class<? extends Flow.Publisher<?>>) clientPublisher.getClass(), ClientConfiguration.defaults())
                    .toCompletableFuture().get();

            System.out.println("DONE!");
        }
    }

    @Test
    public void givenSubscriberWhenPublishEventsWithResultsAndMetadataThenSubscriberCalculatesResults() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreamsServer reactiveStreamsServer = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);
            ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);

            // Server subscriber
            ServerSubscriber<WithResult<WithMetadata<String>, Integer>> serverSubscriber = new ServerSubscriber<>() {
                @Override
                public void onNext(WithResult<WithMetadata<String>, Integer> item) {
                    item.result().complete(item.event().event().length());
                    subscription.request(1);
                }
            };

            reactiveStreamsServer.subscriber("serversubscriber", cfg -> serverSubscriber, (Class<? extends Flow.Subscriber<?>>) serverSubscriber.getClass());

            // Client publisher
            final long total = 1000;
            final ClientPublisher<WithResult<WithMetadata<String>, Integer>> clientPublisher = new ClientPublisher<>() {
                @Override
                protected void publish(Flow.Subscriber<? super WithResult<WithMetadata<String>, Integer>> subscriber) {
                    for (int i = 0; i < total; i++) {
                        CompletableFuture<Integer> future = new CompletableFuture<>();
                        future.whenComplete((r, t) ->
                        {
//                                    System.out.println("Length:"+r);
                        });
                        subscriber.onNext(new WithResult<>(new WithMetadata<>(new Metadata.Builder()
                                .add("foo", "bar")
                                .build(), i + "XORCERY"), future));
                    }
                    subscriber.onComplete();
                }
            };

            long start = System.currentTimeMillis();
            InstanceConfiguration standardConfiguration = new InstanceConfiguration(xorcery.getServiceLocator().getService(Configuration.class).getConfiguration("instance"));
            URI serverUri = standardConfiguration.getURI();
            reactiveStreamsClient.publish(serverUri.getAuthority(), "serversubscriber", Configuration::empty, clientPublisher, (Class<? extends Flow.Publisher<?>>) clientPublisher.getClass(), ClientConfiguration.defaults())
                    .whenComplete((r, t) ->
                            System.out.println("Process complete")).toCompletableFuture().get();

            long end = System.currentTimeMillis();
            long time = end - start;
            System.out.printf("Time: %d, Rate:%d\n", time, total / time);
        }
    }

    public static class StringClientPublisher
            extends ClientPublisher<String> {

        private final int total;

        public StringClientPublisher(int total) {
            this.total = total;
        }

        @Override
        protected void publish(Flow.Subscriber<? super String> subscriber) {
            for (int i = 0; i < total; i++) {
                subscriber.onNext(i + "");
            }
            subscriber.onComplete();
        }
    }

    public static class StringServerSubscriber
            extends ServerSubscriber<String> {
        @Override
        public void onNext(String item) {
//            System.out.println(item);
            subscription.request(1);
        }
    }
}
