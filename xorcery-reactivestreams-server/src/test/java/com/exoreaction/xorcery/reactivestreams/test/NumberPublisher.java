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
package com.exoreaction.xorcery.reactivestreams.test;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NumberPublisher implements Publisher<Long> {

    private final int maxNumbers;

    public NumberPublisher(int maxNumbers) {
        this.maxNumbers = maxNumbers;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        NumberSubscription subscription = new NumberSubscription(subscriber);
        subscriber.onSubscribe(subscription);
    }

    private class NumberSubscription implements Subscription {

        private final Subscriber<? super Long> subscriber;

        private final AtomicLong nr = new AtomicLong();

        private NumberSubscription(Subscriber<? super Long> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public synchronized void request(long n) {
            for (long i = 0; i < n; i++) {
                long number = nr.incrementAndGet();
                if (number < maxNumbers) {
                    subscriber.onNext(number);
                } else {
                    subscriber.onComplete();
                    return;
                }
            }
        }

        @Override
        public void cancel() {
            subscriber.onComplete();
        }
    }
}
