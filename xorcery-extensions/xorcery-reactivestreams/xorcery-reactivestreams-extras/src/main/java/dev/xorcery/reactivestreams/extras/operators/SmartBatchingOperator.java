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
package dev.xorcery.reactivestreams.extras.operators;

import dev.xorcery.concurrent.SmartBatcher;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SmartBatchingOperator {

    public static <T> BiFunction<Flux<T>, ContextView, Publisher<T>> smartBatching(
            BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler,
            Supplier<BlockingQueue<T>> queue,
            Supplier<Executor> executor) {
        return (flux, contextView) -> new SmartBatchingHandler<T>(flux, contextView, handler, queue.get(), executor.get());
    }

    public static <T> BiFunction<Flux<T>, ContextView, Publisher<T>> smartBatching(
            BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler) {
        return smartBatching(handler, ()-> new ArrayBlockingQueue<>(1024), ()->Schedulers.boundedElastic()::schedule);
    }

    private static class SmartBatchingHandler<T>
            implements Publisher<T> {

        private final Flux<T> smartBatchingPublishSubscriber;

        public SmartBatchingHandler(
                Flux<T> upstream,
                ContextView contextView,
                BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler,
                BlockingQueue<T> queue,
                Executor executor) {
            smartBatchingPublishSubscriber = Flux.create(sink -> new SmartBatchingSubscriber<T>(upstream, sink, contextView, handler, queue, executor));
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            smartBatchingPublishSubscriber.subscribe(subscriber);
        }
    }

    private static class SmartBatchingSubscriber<T>
            extends BaseSubscriber<T>
            implements Consumer<Collection<T>>, SynchronousSink<Collection<T>> {

        private final FluxSink<T> downstream;
        private final ContextView contextView;
        private final BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler;
        private final SmartBatcher<T> smartBatcher;

        public SmartBatchingSubscriber(
                Flux<T> upstream,
                FluxSink<T> downstream,
                ContextView contextView, BiConsumer<Collection<T>,
                SynchronousSink<Collection<T>>> handler,
                BlockingQueue<T> queue,
                Executor executor) {
            this.downstream = downstream;
            this.contextView = contextView;
            this.handler = handler;
            smartBatcher = new SmartBatcher<>(this, queue, executor);
            upstream.subscribe(this);

            downstream.onDispose(smartBatcher::close);
            downstream.onCancel(this::cancel);
            downstream.onRequest(this::request);
        }

        @Override
        protected void hookOnNext(T value) {
            try {
                smartBatcher.submit(value);
            } catch (InterruptedException e) {
                downstream.error(e);
            }
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            smartBatcher.close();
            downstream.error(throwable);
        }

        @Override
        protected void hookOnComplete() {
            smartBatcher.close();
            downstream.complete();
        }

        @Override
        protected void hookOnCancel() {
            smartBatcher.close();
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {

        }

        @Override
        public void accept(Collection<T> ts) {
            handler.accept(ts, this);
        }

        // SynchronousSink
        @Override
        public void complete() {
            downstream.complete();
        }

        @Override
        public void error(Throwable e) {
            downstream.error(e);
        }

        @Override
        public void next(Collection<T> ts) {
            ts.forEach(downstream::next);
        }

        @Override
        public Context currentContext() {
            return Context.of(contextView);
        }

        @Override
        public ContextView contextView() {
            return contextView;
        }
    }
}
