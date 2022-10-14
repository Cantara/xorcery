package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.test.fibonacci.FibonacciSequence;
import com.exoreaction.xorcery.service.reactivestreams.test.fibonacci.FibonacciSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.test.media.LongMessageBodyReader;
import com.exoreaction.xorcery.service.reactivestreams.test.media.LongMessageBodyWriter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExternalClientPublisherTest {

    @Test
    @Disabled
    public void thatServerSubscriberGetsAllExpectedClientPublishedFibonacciNumbers() {
        Configuration configuration = new Configuration.Builder()
                .add("server.http.port", "60797")
                .add("server.http2.enabled", "true")
                .add("client.http2.enabled", "true")
                .build();
        JettyAndJerseyBasedTestServer testServer = new JettyAndJerseyBasedTestServer(configuration, List.of(LongMessageBodyWriter.class), List.of(LongMessageBodyReader.class));
        testServer.start();
        try {
            ReactiveStreams reactiveStreams = testServer.getReactiveStreams();

            final int NUMBERS_IN_FIBONACCI_SEQUENCE = 12;

            // server subscribes
            FibonacciSubscriber subscriber = new FibonacciSubscriber();
            CompletableFuture<Void> subscriberComplete = reactiveStreams.subscriber("/fibonacci", config -> subscriber, FibonacciSubscriber.class);
            subscriberComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("subscriber completed!%n");
            });

            // client publishes
            //CompletableFuture<Void> publisherComplete = reactiveStreams.publish(URI.create(String.format("ws://localhost:%d/fibonacci", testServer.getHttpPort())), new Configuration.Builder().build(), new FibonacciPublisher(NUMBERS_IN_FIBONACCI_SEQUENCE), FibonacciPublisher.class);
            //publisherComplete.join();

            subscriber.waitForTermination(15, TimeUnit.MINUTES); // waiting for external publisher to complete publishing

            System.out.printf("publisher completed!%n");

            List<Long> allReceivedNumbers = subscriber.getAllReceivedNumbers();
            assertEquals(FibonacciSequence.sequenceOf(NUMBERS_IN_FIBONACCI_SEQUENCE), allReceivedNumbers);

            Thread.sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            testServer.stop();
        }
    }
}
