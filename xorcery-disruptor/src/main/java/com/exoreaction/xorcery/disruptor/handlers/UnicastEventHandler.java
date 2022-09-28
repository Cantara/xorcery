package com.exoreaction.xorcery.disruptor.handlers;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class UnicastEventHandler<T>
    extends AbstractRoutingEventHandler<T>
{
    @Override
    public void onEvent(T event, long sequence, boolean endOfBatch) throws Exception {
        while (true) {
            for (SubscriptionTracker<T> tracker : subscribers) {
                if (tracker.requests().get()>0)
                {
                    tracker.requests().getAndDecrement();
                    tracker.subscriber().onNext(event);
                    return;
                }
            }

            // Try again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }
}
