package com.exoreaction.xorcery.service.reactivestreams.test.fibonacci;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

public class BinaryFibonacciSubscriber implements Flow.Subscriber<byte[]> {
    private Flow.Subscription subscription;

    private final List<byte[]> receivedNumbers = new Vector<>();

    private final CountDownLatch terminatedLatch = new CountDownLatch(1);

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.printf("received onSubscribe()%n");
        this.subscription = subscription;
        subscription.request(2);
    }

    @Override
    public void onNext(byte[] item) {
        System.out.printf("onNext: %s%n", item);
        receivedNumbers.add(item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.printf("onError%n");
        throwable.printStackTrace();
        terminatedLatch.countDown();
    }

    @Override
    public void onComplete() {
        System.out.printf("onComplete!%n");
        terminatedLatch.countDown();
    }

    public ArrayList<byte[]> getAllReceivedNumbers() {
        return new ArrayList<>(receivedNumbers);
    }

    /**
     * @param timeout
     * @param unit
     * @return true if the subscription has terminated and false if the waiting time elapsed before the subscriber terminated
     * @throws InterruptedException
     */
    public boolean waitForTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminatedLatch.await(timeout, unit);
    }
}
