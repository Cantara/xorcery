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
package com.exoreaction.xorcery.reactivestreams.client;

import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

class SubscribeResultHandler implements Subscriber<Object> {
    private final Subscriber<Object> subscriber;
    private CompletableFuture<Void> result;
    private MessageWriter<Object> writer;
    private MessageReader<Object> reader;
    private Subscription subscription;

    public SubscribeResultHandler(Subscriber<Object> subscriber, CompletableFuture<Void> result) {
        this.subscriber = subscriber;
        this.result = result;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(Object item) {
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
        result.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
        result.complete(null);
    }
}
