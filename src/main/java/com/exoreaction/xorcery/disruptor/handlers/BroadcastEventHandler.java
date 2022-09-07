package com.exoreaction.xorcery.disruptor.handlers;

import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class BroadcastEventHandler<T>
        extends AbstractRoutingEventHandler<T> {
    @Override
    public void onEvent(T event, long sequence, boolean endOfBatch) throws Exception {
        while (true) {
            for (SubscriptionTracker<T> tracker : subscribers) {
                while (tracker.requests().get() <= 0) {
                    // Wait for subscriber to request
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }

                tracker.requests().getAndDecrement();
                tracker.subscriber().onNext(event);
            }
        }
    }
}
