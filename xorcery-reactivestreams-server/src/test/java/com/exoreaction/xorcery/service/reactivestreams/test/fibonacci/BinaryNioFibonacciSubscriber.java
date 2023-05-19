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
package com.exoreaction.xorcery.service.reactivestreams.test.fibonacci;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

public class BinaryNioFibonacciSubscriber implements Flow.Subscriber<ByteBuffer> {
    private Flow.Subscription subscription;

    private final List<ByteBuffer> receivedNumbers = new Vector<>();

    private final CountDownLatch terminatedLatch = new CountDownLatch(1);

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.printf("received onSubscribe()%n");
        this.subscription = subscription;
        subscription.request(2);
    }

    @Override
    public void onNext(ByteBuffer item) {
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

    public ArrayList<ByteBuffer> getAllReceivedNumbers() {
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
