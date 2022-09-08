package com.exoreaction.xorcery.disruptor.handlers;

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
