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

import org.apache.logging.log4j.LogManager;

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
        LogManager.getLogger(getClass()).info("received onSubscribe()");
        this.subscription = subscription;
        subscription.request(2);
    }

    @Override
    public void onNext(byte[] item) {
        LogManager.getLogger(getClass()).info("onNext: {}", item);
        receivedNumbers.add(item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        LogManager.getLogger(getClass()).error("onError", throwable);
        terminatedLatch.countDown();
    }

    @Override
    public void onComplete() {
        LogManager.getLogger(getClass()).info("onComplete");
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
