package com.exoreaction.xorcery.service.reactivestreams.test.fibonacci;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Flow;

public class FibonacciSubscriber implements Flow.Subscriber<Long> {
    private Flow.Subscription subscription;

    private final List<Long> receivedNumbers = new Vector<>();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.printf("received onSubscribe()%n");
        this.subscription = subscription;
        subscription.request(2);
    }

    @Override
    public void onNext(Long item) {
        System.out.printf("onNext: %d%n", item);
        receivedNumbers.add(item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.printf("onError%n");
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        System.out.printf("onComplete!%n");
    }

    public List<Long> getAllReceivedNumbers() {
        return new ArrayList<>(receivedNumbers);
    }
}
