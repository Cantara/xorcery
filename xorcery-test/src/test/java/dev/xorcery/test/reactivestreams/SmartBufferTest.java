package dev.xorcery.test.reactivestreams;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import dev.xorcery.concurrent.NamedThreadFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.IntStream;

@Disabled
public class SmartBufferTest {

    @Test
    public void testDisruptor() {
        AtomicLong counter = new AtomicLong();
        AtomicLong requests = new AtomicLong();
        Flux.fromStream(IntStream.range(0, 10000000).boxed())
                .doOnRequest(r -> System.out.println("Source requests:" + requests.addAndGet(r)))
                .doOnNext(v -> requests.decrementAndGet())
                .doOnNext(v ->
                {
                    int mod = 1000;
                    if (v % mod == 0) {
                        System.out.println("Switch " + v);
                    }

/*
                    if ((v / mod) % 2 == 1) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
*/
                })
                .publishOn(Schedulers.boundedElastic(), 4096)
                .transformDeferred(new DisruptorFactory<>())
//                .doOnRequest(r -> System.out.println("Disruptor request:"+r))
                .<List<Integer>>handle((batch, sink) ->
                {

/*
                    try {
                        Thread.sleep(3);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
*/
/*
                    if ((counter.get() / 1000) % 2 == 1) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
*/
                    sink.next(batch);
                })
                .doOnNext(value -> {
                    for (Integer i : value) {
                        if (i % 10000 == 0)
                            System.out.println("Stream:" + i);
                    }
//                        if (value.size() > 10)
//                    System.out.println("Size:" + value.size());
                })
                .flatMapIterable(v->v, 4096)
                .doOnRequest(r -> System.out.println("Subscriber request:" + r))
                .doOnNext(v -> counter.incrementAndGet())
                .subscribeOn(Schedulers.parallel(), true)
                .blockLast();

        System.out.println("Counter:" + counter.get());
    }

    private static class DisruptorFactory<T>
            implements Function<Flux<T>, Publisher<List<T>>> {
        @Override
        public Publisher<List<T>> apply(Flux<T> flux) {
            return new DisruptorHandler<>(flux);
        }
    }

    private static class DisruptorHandler<T>
            implements Publisher<List<T>> {
        private final Flux<List<T>> disruptorPublishSubscriber;

        public DisruptorHandler(Flux<T> upstream) {
            disruptorPublishSubscriber = Flux.<List<T>>create(sink ->
            {
                new DisruptorSubscriber<>(upstream, sink);
            });
        }

        @Override
        public void subscribe(Subscriber<? super List<T>> subscriber) {
            disruptorPublishSubscriber.subscribe(subscriber);
        }
    }

    private static class DisruptorSubscriber<T>
            extends BaseSubscriber<T>
            implements EventHandler<EventReference<T>> {
        private final Disruptor<EventReference<T>> disruptor;
        private final FluxSink<List<T>> downstream;
        private List<T> list;

        public DisruptorSubscriber(Flux<T> upstream, FluxSink<List<T>> downstream) {
            this.downstream = downstream;
            disruptor = new Disruptor<>(EventReference::new, 4096, new NamedThreadFactory("DisruptorFlux-"),
                    ProducerType.MULTI,
                    new BlockingWaitStrategy());
            disruptor.handleEventsWith(this);
            disruptor.start();
            upstream.subscribe(this);

            downstream.onRequest(upstream()::request);
            downstream.onCancel(upstream()::cancel);
            downstream.onDispose(disruptor::shutdown);
        }

        @Override
        protected void hookOnNext(T value) {
            disruptor.publishEvent((ref, seq, val) ->
            {
                ref.instance = val;
            }, value);
        }

        @Override
        protected void hookOnComplete() {
            disruptor.shutdown();
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
        public void onEvent(EventReference<T> event, long sequence, boolean endOfBatch) throws Exception {
            list.add(event.instance);
            if (endOfBatch) {
                downstream.next(list);
//                System.out.println("End of batch:"+list.size());
                request(list.size() - 1);
                list = null;
            }
        }
    }

    private static class EventReference<T> {
        public T instance;
    }
}
