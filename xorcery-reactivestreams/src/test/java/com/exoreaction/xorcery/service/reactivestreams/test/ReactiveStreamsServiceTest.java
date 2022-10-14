package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.test.fibonacci.FibonacciPublisher;
import com.exoreaction.xorcery.service.reactivestreams.test.fibonacci.FibonacciSequence;
import com.exoreaction.xorcery.service.reactivestreams.test.fibonacci.FibonacciSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.test.media.LongMessageBodyReader;
import com.exoreaction.xorcery.service.reactivestreams.test.media.LongMessageBodyWriter;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactiveStreamsServiceTest {

    @Test
    public void thatSingleClientSubscriberGetsAllExpectedServerPublishedFibonacciNumbers() {
        thatClientSubscribersGetsAllExpectedServerPublishedFibonacciNumbers(12, 1);
    }

    @Test
    public void thatMultipleClientSubscribersGetsAllExpectedServerPublishedFibonacciNumbers() {
        thatClientSubscribersGetsAllExpectedServerPublishedFibonacciNumbers(12, 3);
    }

    private void thatClientSubscribersGetsAllExpectedServerPublishedFibonacciNumbers(final int numbersInFibonacciSequence, final int numberOfSubscribers) {
        Configuration configuration = new Configuration.Builder()
                .add("server.http2.enabled", "true")
                .add("client.http2.enabled", "true")
                .build();
        JettyAndJerseyBasedTestServer testServer = new JettyAndJerseyBasedTestServer(configuration, List.of(LongMessageBodyWriter.class), List.of(LongMessageBodyReader.class));
        testServer.start();
        try {
            ReactiveStreams reactiveStreams = testServer.getReactiveStreams();

            // server publishes
            CompletableFuture<Void> publisherComplete = reactiveStreams.publisher("/fibonacci", config -> new FibonacciPublisher(numbersInFibonacciSequence), FibonacciPublisher.class);
            publisherComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("publisher completed!%n");
            });

            // client subscribes
            CompletableFuture<Void>[] subscriberCompleteArray = new CompletableFuture[numberOfSubscribers];
            for (int i = 0; i < numberOfSubscribers; i++) {
                FibonacciSubscriber subscriber = new FibonacciSubscriber();
                CompletableFuture<Void> future = reactiveStreams.subscribe(URI.create(String.format("ws://localhost:%d/fibonacci", testServer.getHttpPort())), Configuration.empty(), subscriber, FibonacciSubscriber.class)
                        .thenAccept(v -> {
                            ArrayList<Long> allReceivedNumbers = subscriber.getAllReceivedNumbers();
                            if (!new ArrayList<>(FibonacciSequence.sequenceOf(numbersInFibonacciSequence)).equals(allReceivedNumbers)) {
                                throw new RuntimeException("Bad list!");
                            }
                        });
                subscriberCompleteArray[i] = future;
            }
            CompletableFuture.allOf(subscriberCompleteArray)
                    .join();
            System.out.printf("subscriber completed!%n");

            Thread.sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            testServer.stop();
        }
    }

    @Test
    public void thatServerSubscriberGetsAllExpectedClientPublishedFibonacciNumbers() {
        Configuration configuration = new Configuration.Builder()
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
            CompletableFuture<Void> publisherComplete = reactiveStreams.publish(URI.create(String.format("ws://localhost:%d/fibonacci", testServer.getHttpPort())), new Configuration.Builder().build(), new FibonacciPublisher(NUMBERS_IN_FIBONACCI_SEQUENCE), FibonacciPublisher.class);
            publisherComplete.join();

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
