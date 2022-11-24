package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.util.Sockets;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.test.fibonacci.*;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests designed to cover all variants of publisher and subscriber web socket endpoint classes and their serialization
 * when event-readers and writers are present or not.
 */
public class ReactiveStreamsServiceTest {

    @Test
    public void thatSingleClientSubscriberGetsAllExpectedServerPublishedFibonacciNumbers() throws Exception {
        thatClientSubscribersGetsAllExpectedServerPublishedFibonacciNumbers(12, 1);
    }

    @Test
    public void thatMultipleClientSubscribersGetsAllExpectedServerPublishedFibonacciNumbers() throws Exception {
        thatClientSubscribersGetsAllExpectedServerPublishedFibonacciNumbers(12, 3);
    }

    private void thatClientSubscribersGetsAllExpectedServerPublishedFibonacciNumbers(final int numbersInFibonacciSequence, final int numberOfSubscribers) throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("server.http.port", Sockets.nextFreePort())
                .build();
        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreams reactiveStreams = xorcery.getServiceLocator().getService(ReactiveStreams.class);

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
                CompletableFuture<Void> future = reactiveStreams.subscribe(UriBuilder.fromUri(standardConfiguration.getServerUri()).scheme("ws").path("fibonacci").build(), Configuration.empty(), subscriber, FibonacciSubscriber.class)
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
        }
    }

    @Test
    public void thatServerSubscriberGetsAllExpectedClientPublishedFibonacciNumbers() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("server.http.port", Sockets.nextFreePort())
                .build();
        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreams reactiveStreams = xorcery.getServiceLocator().getService(ReactiveStreams.class);

            final int NUMBERS_IN_FIBONACCI_SEQUENCE = 12;

            // server subscribes
            FibonacciSubscriber subscriber = new FibonacciSubscriber();
            CompletableFuture<Void> subscriberComplete = reactiveStreams.subscriber("/fibonacci", config -> subscriber, FibonacciSubscriber.class);
            subscriberComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("subscriber completed!%n");
            });

            // client publishes
            CompletableFuture<Void> publisherComplete = reactiveStreams.publish(UriBuilder.fromUri(standardConfiguration.getServerUri()).scheme("ws").path("fibonacci").build(), Configuration.empty(), new FibonacciPublisher(NUMBERS_IN_FIBONACCI_SEQUENCE), FibonacciPublisher.class);
            publisherComplete.join();

            System.out.printf("publisher completed!%n");

            List<Long> allReceivedNumbers = subscriber.getAllReceivedNumbers();
            assertEquals(FibonacciSequence.sequenceOf(NUMBERS_IN_FIBONACCI_SEQUENCE), allReceivedNumbers);

            Thread.sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void thatSingleClientSubscriberGetsAllExpectedServerPublishedBinaryFibonacciNumbers() throws Exception {
        thatClientSubscribersGetsAllExpectedServerPublishedBinaryFibonacciNumbers(12, 1);
    }

    @Test
    public void thatMultipleClientSubscribersGetsAllExpectedServerPublishedBinaryFibonacciNumbers() throws Exception {
        thatClientSubscribersGetsAllExpectedServerPublishedBinaryFibonacciNumbers(12, 3);
    }

    private void thatClientSubscribersGetsAllExpectedServerPublishedBinaryFibonacciNumbers(final int numbersInFibonacciSequence, final int numberOfSubscribers) throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("server.http.port", Sockets.nextFreePort())
                .build();
        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreams reactiveStreams = xorcery.getServiceLocator().getService(ReactiveStreams.class);

            // server publishes
            CompletableFuture<Void> publisherComplete = reactiveStreams.publisher("/fibonacci", config -> new BinaryFibonacciPublisher(numbersInFibonacciSequence), BinaryFibonacciPublisher.class);
            publisherComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("publisher completed!%n");
            });

            // client subscribes
            CompletableFuture<Void>[] subscriberCompleteArray = new CompletableFuture[numberOfSubscribers];
            for (int i = 0; i < numberOfSubscribers; i++) {
                BinaryFibonacciSubscriber subscriber = new BinaryFibonacciSubscriber();
                CompletableFuture<Void> future = reactiveStreams.subscribe(UriBuilder.fromUri(standardConfiguration.getServerUri()).scheme("ws").path("fibonacci").build(), Configuration.empty(), subscriber, BinaryFibonacciSubscriber.class)
                        .thenAccept(v -> {
                            ArrayList<byte[]> allReceivedNumbers = subscriber.getAllReceivedNumbers();
                            byte[][] allReceivedNumbersArray = allReceivedNumbers.toArray(new byte[0][0]);
                            byte[][] expectedArray = FibonacciSequence.binarySequenceOf(numbersInFibonacciSequence).toArray(new byte[0][0]);
                            if (!Arrays.deepEquals(expectedArray, allReceivedNumbersArray)) {
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
        }
    }

    @Test
    public void thatServerSubscriberGetsAllExpectedClientPublishedBinaryFibonacciNumbers() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("server.http.port", Sockets.nextFreePort())
                .build();
        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreams reactiveStreams = xorcery.getServiceLocator().getService(ReactiveStreams.class);

            final int NUMBERS_IN_FIBONACCI_SEQUENCE = 12;

            // server subscribes
            BinaryFibonacciSubscriber subscriber = new BinaryFibonacciSubscriber();
            CompletableFuture<Void> subscriberComplete = reactiveStreams.subscriber("/fibonacci", config -> subscriber, BinaryFibonacciSubscriber.class);
            subscriberComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("subscriber completed!%n");
            });

            // client publishes
            CompletableFuture<Void> publisherComplete = reactiveStreams.publish(UriBuilder.fromUri(standardConfiguration.getServerUri()).scheme("ws").path("fibonacci").build(), Configuration.empty(), new BinaryFibonacciPublisher(NUMBERS_IN_FIBONACCI_SEQUENCE), BinaryFibonacciPublisher.class);
            publisherComplete.join();

            System.out.printf("publisher completed!%n");

            List<byte[]> allReceivedNumbers = subscriber.getAllReceivedNumbers();
            byte[][] allReceivedNumbersArray = allReceivedNumbers.toArray(new byte[0][0]);
            byte[][] expectedArray = FibonacciSequence.binarySequenceOf(NUMBERS_IN_FIBONACCI_SEQUENCE).toArray(new byte[0][0]);
            assertArrayEquals(expectedArray, allReceivedNumbersArray);

            Thread.sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void thatSingleClientSubscriberGetsAllExpectedServerPublishedBinaryNioFibonacciNumbers() throws Exception {
        thatClientSubscribersGetsAllExpectedServerPublishedBinaryNioFibonacciNumbers(12, 1);
    }

    @Test
    public void thatMultipleClientSubscribersGetsAllExpectedServerPublishedBinaryNioFibonacciNumbers() throws Exception {
        thatClientSubscribersGetsAllExpectedServerPublishedBinaryNioFibonacciNumbers(12, 3);
    }

    private void thatClientSubscribersGetsAllExpectedServerPublishedBinaryNioFibonacciNumbers(final int numbersInFibonacciSequence, final int numberOfSubscribers) throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("server.http.port", Sockets.nextFreePort())
                .build();
        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreams reactiveStreams = xorcery.getServiceLocator().getService(ReactiveStreams.class);

            // server publishes
            CompletableFuture<Void> publisherComplete = reactiveStreams.publisher("/fibonacci", config -> new BinaryNioFibonacciPublisher(numbersInFibonacciSequence), BinaryNioFibonacciPublisher.class);
            publisherComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("publisher completed!%n");
            });

            // client subscribes
            CompletableFuture<Void>[] subscriberCompleteArray = new CompletableFuture[numberOfSubscribers];
            for (int i = 0; i < numberOfSubscribers; i++) {
                BinaryNioFibonacciSubscriber subscriber = new BinaryNioFibonacciSubscriber();
                CompletableFuture<Void> future = reactiveStreams.subscribe(UriBuilder.fromUri(standardConfiguration.getServerUri()).scheme("ws").path("fibonacci").build(), Configuration.empty(), subscriber, BinaryNioFibonacciSubscriber.class)
                        .thenAccept(v -> {
                            ArrayList<byte[]> allReceivedNumbers = new ArrayList<>(subscriber.getAllReceivedNumbers().stream().map(ByteBuffer::array).toList());
                            byte[][] allReceivedNumbersArray = allReceivedNumbers.toArray(new byte[0][0]);
                            byte[][] expectedArray = FibonacciSequence.binarySequenceOf(numbersInFibonacciSequence).toArray(new byte[0][0]);
                            if (!Arrays.deepEquals(expectedArray, allReceivedNumbersArray)) {
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
        }
    }

    @Test
    public void thatServerSubscriberGetsAllExpectedClientPublishedBinaryNioFibonacciNumbers() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("server.http.port", Sockets.nextFreePort())
                .build();
        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            ReactiveStreams reactiveStreams = xorcery.getServiceLocator().getService(ReactiveStreams.class);

            final int NUMBERS_IN_FIBONACCI_SEQUENCE = 12;

            // server subscribes
            BinaryNioFibonacciSubscriber subscriber = new BinaryNioFibonacciSubscriber();
            CompletableFuture<Void> subscriberComplete = reactiveStreams.subscriber("/fibonacci", config -> subscriber, BinaryNioFibonacciSubscriber.class);
            subscriberComplete.thenAccept(v -> {
                // TODO figure out why this never happens!
                System.out.printf("subscriber completed!%n");
            });

            // client publishes
            CompletableFuture<Void> publisherComplete = reactiveStreams.publish(UriBuilder.fromUri(standardConfiguration.getServerUri()).scheme("ws").path("fibonacci").build(), Configuration.empty(), new BinaryNioFibonacciPublisher(NUMBERS_IN_FIBONACCI_SEQUENCE), BinaryNioFibonacciPublisher.class);
            publisherComplete.join();

            System.out.printf("publisher completed!%n");

            List<ByteBuffer> allReceivedNumbers = subscriber.getAllReceivedNumbers();
            List<ByteBuffer> expectedBytes = FibonacciSequence.binaryNioSequenceOf(NUMBERS_IN_FIBONACCI_SEQUENCE);
            assertEquals(expectedBytes, allReceivedNumbers);

            Thread.sleep(100);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
