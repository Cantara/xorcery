package com.exoreaction.xorcery.reactivestreams.disruptor;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Transformer to be used with {@link Flux#transformDeferredContextual(BiFunction)}.
 * <p>
 * Converts incoming items to batches of items using a configurable Disruptor which are then delegated to a function. Primary use of this
 * is to support smart batching.
 *
 * @param <T>
 */
public class SmartBatching<T>
        implements BiFunction<Flux<T>, ContextView, Publisher<T>> {

    private final DisruptorConfiguration configuration;
    private final BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler;

    public SmartBatching(DisruptorConfiguration configuration, BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler) {
        this.configuration = configuration;
        this.handler = handler;
    }

    @Override
    public Publisher<T> apply(Flux<T> flux, ContextView contextView) {
        return new DisruptorHandler(flux, contextView, handler);
    }

    private class DisruptorHandler
            implements Publisher<T> {
        private final Flux<T> disruptorPublishSubscriber;

        public DisruptorHandler(Flux<T> upstream, ContextView contextView, BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler) {
            disruptorPublishSubscriber = Flux.push(sink ->
                    new DisruptorSubscriber(upstream, sink, contextView, handler));
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            disruptorPublishSubscriber.subscribe(subscriber);
        }
    }

    private class DisruptorSubscriber
            extends BaseSubscriber<T>
            implements EventHandler<EventReference<T>>, SynchronousSink<Collection<T>> {
        private final Disruptor<EventReference<T>> disruptor;
        private final FluxSink<T> downstream;
        private final ContextView contextView;
        private final BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler;
        private final List<T> list = new ArrayList<>(configuration.getSize());

        public DisruptorSubscriber(Flux<T> upstream, FluxSink<T> downstream, ContextView contextView, BiConsumer<Collection<T>, SynchronousSink<Collection<T>>> handler) {
            this.downstream = downstream;
            this.contextView = contextView;
            this.handler = handler;
            disruptor = new Disruptor<>(EventReference::new, configuration.getSize(), new NamedThreadFactory(configuration.getPrefix()),
                    configuration.getProducerType(),
                    configuration.getWaitStrategy());
            disruptor.handleEventsWith(this);
            disruptor.start();
            upstream.subscribe(this);

            downstream.onRequest(upstream()::request);
            downstream.onCancel(upstream()::cancel);
            downstream.onDispose(disruptor::shutdown);
        }

        @Override
        protected void hookOnNext( T value) {
            disruptor.publishEvent((ref, seq, val) ->
                    ref.instance = val, value);
        }

        @Override
        protected void hookOnComplete() {
            try {
                disruptor.shutdown(configuration.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // Ignore
            }
            downstream.complete();
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            try {
                disruptor.shutdown(configuration.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // Ignore
            }
            downstream.error(throwable);
        }

        @Override
        protected void hookOnCancel() {
            disruptor.shutdown();
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
        }

        @Override
        public void onBatchStart(long batchSize, long queueDepth) {
            list.clear();
        }

        @Override
        public void onEvent(EventReference<T> event, long sequence, boolean endOfBatch) {
            list.add(event.instance);
            if (endOfBatch) {
                handler.accept(list, this);
            }
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

    private static class EventReference<T> {
        public T instance;
    }
}
