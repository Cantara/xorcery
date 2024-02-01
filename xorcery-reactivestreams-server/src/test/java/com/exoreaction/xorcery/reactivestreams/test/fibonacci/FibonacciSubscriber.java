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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FibonacciSubscriber implements Subscriber<Long> {

    private Subscription subscription;
    private final List<Long> receivedNumbers = new ArrayList<>();
    private final CompletableFuture<List<Long>> result = new CompletableFuture<>();

    @Override
    public void onSubscribe(Subscription subscription) {
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
        result.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        result.complete(receivedNumbers);
    }

    public CompletableFuture<List<Long>> getAllReceivedNumbers() {
        return result;
    }
}
