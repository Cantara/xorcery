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
package com.exoreaction.xorcery.disruptor.handlers;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class BroadcastEventHandler<T>
        extends AbstractRoutingEventHandler<T> {

    private boolean dropIfNoSubscribers;

    public BroadcastEventHandler(boolean dropIfNoSubscribers) {
        this.dropIfNoSubscribers = dropIfNoSubscribers;
    }

    @Override
    public void onEvent(T event, long sequence, boolean endOfBatch) throws Exception {
        while (subscribers.isEmpty() && !dropIfNoSubscribers)
            // Wait for at least one subscriber
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                return;
            }

        for (SubscriptionTracker<T> tracker : subscribers) {
            while (tracker.requests().get() <= 0) {
                // Wait for subscriber to request
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    return;
                }
            }

            tracker.requests().decrementAndGet();
            tracker.subscriber().onNext(event);
        }
    }
}
