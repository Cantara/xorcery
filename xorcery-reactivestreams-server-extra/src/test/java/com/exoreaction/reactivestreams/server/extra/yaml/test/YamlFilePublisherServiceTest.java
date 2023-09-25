package com.exoreaction.reactivestreams.server.extra.yaml.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.server.ReactiveStreamsServerConfiguration;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.BaseSubscriber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class YamlFilePublisherServiceTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void testYamlFilePublisher() {
        Configuration configuration = xorceryExtension.getConfiguration();
        ReactiveStreamsServerConfiguration rssc = ReactiveStreamsServerConfiguration.get(configuration);

        TestEventSubscriber subscriber = new TestEventSubscriber();
        CompletableFuture<Void> result = xorceryExtension.getServiceLocator().getService(ReactiveStreamsClient.class)
                .subscribe(rssc.getURI(),
                        "testevents",
                        () -> Configuration.empty(),
                        subscriber,
                        subscriber.getClass(),
                        ClientConfiguration.defaults());

        result.orTimeout(10, TimeUnit.SECONDS).join();

        Assertions.assertEquals(4, subscriber.count);
    }

    public class TestEventSubscriber
            extends BaseSubscriber<WithMetadata<ArrayNode>> {

        int count;

        @Override
        protected void hookOnNext(WithMetadata<ArrayNode> value) {
            count++;
//            System.out.println(value);
        }

        @Override
        protected void hookOnComplete() {
            cancel();
        }
    }
}
