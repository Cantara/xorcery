package com.exoreaction.xorcery.disruptor.reactor;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.disruptor.DisruptorConfiguration;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Transformer to be used with {@link Flux#transformDeferred(Function)}.
 *
 * Converts incoming items to batches of items using a configurable Disruptor. Primary use of this
 * is to support smart batching.
 *
 * @param <T>
 */
public class SmartBatchingTransformer<T>
        implements Function<Flux<T>, Publisher<List<T>>> {

    private final DisruptorConfiguration configuration;

    public SmartBatchingTransformer(DisruptorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Publisher<List<T>> apply(Flux<T> flux) {
        return new DisruptorHandler(flux);
    }

    private class DisruptorHandler
            implements Publisher<List<T>> {
        private final Flux<List<T>> disruptorPublishSubscriber;

        public DisruptorHandler(Flux<T> upstream) {
            disruptorPublishSubscriber = Flux.<List<T>>push(sink ->
            {
                new DisruptorSubscriber(upstream, sink);
            });
        }

        @Override
        public void subscribe(Subscriber<? super List<T>> subscriber) {
            disruptorPublishSubscriber.subscribe(subscriber);
        }
    }

    private class DisruptorSubscriber
            extends BaseSubscriber<T>
            implements EventHandler<EventReference<T>> {
        private final Disruptor<EventReference<T>> disruptor;
        private final FluxSink<List<T>> downstream;
        private List<T> list;

        public DisruptorSubscriber(Flux<T> upstream, FluxSink<List<T>> downstream) {
            this.downstream = downstream;
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
            {
                ref.instance = val;
            }, value);
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
            disruptor.shutdown();
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
            list = new ArrayList<>((int) batchSize);
        }

        @Override
        public void onEvent(EventReference<T> event, long sequence, boolean endOfBatch) {
            list.add(event.instance);
            if (endOfBatch) {
                downstream.next(list);
                request(list.size() - 1);
                list = null;
            }
        }
    }

    private static class EventReference<T> {
        public T instance;
    }
}
