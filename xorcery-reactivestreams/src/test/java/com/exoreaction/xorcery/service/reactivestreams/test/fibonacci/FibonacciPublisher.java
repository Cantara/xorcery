package com.exoreaction.xorcery.service.reactivestreams.test.fibonacci;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FibonacciPublisher implements Flow.Publisher<Long> {

    private final int maxNumbersInFibonacciSequence;

    private final List<MySubscription> subscriptions = new ArrayList<>();

    public FibonacciPublisher(int maxNumbersInFibonacciSequence) {
        this.maxNumbersInFibonacciSequence = maxNumbersInFibonacciSequence;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Long> subscriber) {
        MySubscription subscription = new MySubscription(subscriber);
        subscriber.onSubscribe(subscription);
        subscription.start();
        subscriptions.add(subscription);
    }

    private class MySubscription implements Flow.Subscription, Runnable {

        private static final AtomicInteger nextId = new AtomicInteger(1);
        private final int id;
        private final Flow.Subscriber<? super Long> subscriber;
        private final AtomicLong budget = new AtomicLong(0);

        private final Iterator<Long> fibonacciSequence;
        private final Thread thread;

        private volatile boolean cancelled = false;

        private final Object sync = new Object();

        private MySubscription(Flow.Subscriber<? super Long> subscriber) {
            this.id = nextId.getAndIncrement();
            FibonacciSequence fibonacciSequence = new FibonacciSequence(maxNumbersInFibonacciSequence);
            this.fibonacciSequence = fibonacciSequence.iterator();
            this.subscriber = subscriber;
            this.thread = new Thread(this);
        }

        private void start() {
            thread.start();
        }

        private void stop() {
            cancelled = true;
            try {
                thread.join();
            } catch (InterruptedException e) {
                thread.interrupt(); // reset interrupt status and ignore
            }
        }

        @Override
        public void run() {
            try {
                while (fibonacciSequence.hasNext()) {
                    if (cancelled) {
                        return;
                    }
                    if (budget.get() > 0) {
                        subscriber.onNext(fibonacciSequence.next());
                        budget.decrementAndGet();
                    }
                    synchronized (sync) {
                        sync.wait(100);
                    }
                }
                subscriber.onComplete();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            } finally {
                subscriptions.remove(this);
            }
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                return;
            }
            budget.addAndGet(n);
            synchronized (sync) {
                sync.notify();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MySubscription that = (MySubscription) o;

            return id == that.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }
}
