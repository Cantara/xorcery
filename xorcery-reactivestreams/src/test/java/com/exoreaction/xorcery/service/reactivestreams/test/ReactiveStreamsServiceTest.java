package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.test.fibonacci.FibonacciPublisher;
import com.exoreaction.xorcery.service.reactivestreams.test.fibonacci.FibonacciSubscriber;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactiveStreamsServiceTest {

    @Test
    public void thatClientSubscriberGetsAllExpectedServerPublishedFibonacciNumbers() {
        JettyAndJerseyBasedTestServer testServer = new JettyAndJerseyBasedTestServer();
        testServer.start();
        try {
            ReactiveStreams reactiveStreams = testServer.getReactiveStreams();

            final int NUMBERS_IN_FIBONACCI_SEQUENCE = 12;

            // server publishes
            CompletableFuture<Void> publisherComplete = reactiveStreams.publisher("/fibonacci", config -> new FibonacciPublisher(NUMBERS_IN_FIBONACCI_SEQUENCE), FibonacciPublisher.class);
            publisherComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("publisher completed!%n");
            });

            // client subscribes
            FibonacciSubscriber subscriber = new FibonacciSubscriber();
            CompletableFuture<Void> subscriberComplete = reactiveStreams.subscribe(URI.create(String.format("ws://localhost:%d/fibonacci", testServer.getHttpPort())), new Configuration.Builder().build(), subscriber, FibonacciSubscriber.class);
            subscriberComplete.join();

            System.out.printf("subscriber completed!%n");

            List<Long> allReceivedNumbers = subscriber.getAllReceivedNumbers();
            assertEquals(List.of(0L, 1L, 1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L, 55L, 89L), allReceivedNumbers);

            Thread.sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            testServer.stop();
        }
    }

    @Test
    public void thatServerSubscriberGetsAllExpectedClientPublishedFibonacciNumbers() {
        JettyAndJerseyBasedTestServer testServer = new JettyAndJerseyBasedTestServer();
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
            CompletableFuture<Void> publisherComplete = reactiveStreams.publish(URI.create(String.format("ws://localhost:%d/fibonacci", testServer.getHttpPort())), new Configuration.Builder().build(), new FibonacciPublisher(NUMBERS_IN_FIBONACCI_SEQUENCE), FibonacciPublisher.class);
            publisherComplete.join();

            System.out.printf("publisher completed!%n");

            List<Long> allReceivedNumbers = subscriber.getAllReceivedNumbers();
            assertEquals(List.of(0L, 1L, 1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L, 55L, 89L), allReceivedNumbers);

            Thread.sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            testServer.stop();
        }
    }
}
