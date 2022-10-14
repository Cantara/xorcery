package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.ByteBufferMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.ByteBufferMessageBodyWriter;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.test.json.JsonByteBufferSubscriber;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ExternalJsonClientPublisherTest {

    @Test
    @Disabled
    public void thatServerSubscriberGetsAllExpectedClientPublishedFibonacciNumbers() {
        Configuration configuration = new Configuration.Builder()
                .add("server.http.port", "60798")
                .add("server.http2.enabled", "true")
                .add("client.http2.enabled", "true")
                .build();
        JettyAndJerseyBasedTestServer testServer = new JettyAndJerseyBasedTestServer(configuration, List.of(ByteBufferMessageBodyWriter.class), List.of(ByteBufferMessageBodyReader.class));
        testServer.start();
        try {
            ReactiveStreams reactiveStreams = testServer.getReactiveStreams();

            // server subscribes
            JsonByteBufferSubscriber subscriber = new JsonByteBufferSubscriber();
            CompletableFuture<Void> subscriberComplete = reactiveStreams.subscriber("/jsonevents", config -> subscriber, JsonByteBufferSubscriber.class);
            subscriberComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("subscriber completed!%n");
            });

            subscriber.waitForTermination(15, TimeUnit.MINUTES); // waiting for external publisher to complete publishing

            System.out.printf("publisher completed!%n");

            List<JsonNode> allReceivedNumbers = subscriber.getAllReceivedEvents();

            Thread.sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            testServer.stop();
        }
    }
}
