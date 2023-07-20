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
package com.exoreaction.xorcery.reactivestreams.test.fibonacci;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BinaryFibonacciPublisher implements Publisher<byte[]> {

    private final int maxNumbersInFibonacciSequence;

    private final List<MySubscription> subscriptions = new ArrayList<>();

    public BinaryFibonacciPublisher(int maxNumbersInFibonacciSequence) {
        this.maxNumbersInFibonacciSequence = maxNumbersInFibonacciSequence;
    }

    @Override
    public void subscribe(Subscriber<? super byte[]> subscriber) {
        MySubscription subscription = new MySubscription(subscriber);
        subscriber.onSubscribe(subscription);
        subscription.start();
        subscriptions.add(subscription);
    }

    private class MySubscription implements Subscription, Runnable {

        private static final AtomicInteger nextId = new AtomicInteger(1);
        private final int id;
        private final Subscriber<? super byte[]> subscriber;
        private final AtomicLong budget = new AtomicLong(0);

        private final Iterator<byte[]> fibonacciSequence;
        private final Thread thread;

        private volatile boolean cancelled = false;

        private final Object sync = new Object();

        private MySubscription(Subscriber<? super byte[]> subscriber) {
            this.id = nextId.getAndIncrement();
            FibonacciSequence fibonacciSequence = new FibonacciSequence(maxNumbersInFibonacciSequence);
            this.fibonacciSequence = fibonacciSequence.binaryIterator();
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
