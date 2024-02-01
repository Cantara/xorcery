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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FibonacciPublisher implements Publisher<Long> {

    private final int maxNumbersInFibonacciSequence;

    public FibonacciPublisher(int maxNumbersInFibonacciSequence) {
        this.maxNumbersInFibonacciSequence = maxNumbersInFibonacciSequence;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        MySubscription subscription = new MySubscription(subscriber);
        subscriber.onSubscribe(subscription);
    }

    private class MySubscription implements Subscription {

        private static final AtomicInteger nextId = new AtomicInteger(1);
        private final int id;
        private final Subscriber<? super Long> subscriber;

        private final Iterator<Long> fibonacciSequence;

        private MySubscription(Subscriber<? super Long> subscriber) {
            this.id = nextId.getAndIncrement();
            FibonacciSequence fibonacciSequence = new FibonacciSequence(maxNumbersInFibonacciSequence);
            this.fibonacciSequence = fibonacciSequence.iterator();
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                return;
            }
            System.out.println("Request "+n+" "+fibonacciSequence.hasNext());
            for (long i = 0; i < n; i++) {
                if (fibonacciSequence.hasNext())
                {
                    subscriber.onNext(fibonacciSequence.next());
                } else
                {
                    subscriber.onComplete();
                    return;
                }
            }
        }

        @Override
        public void cancel() {
            subscriber.onComplete();
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
