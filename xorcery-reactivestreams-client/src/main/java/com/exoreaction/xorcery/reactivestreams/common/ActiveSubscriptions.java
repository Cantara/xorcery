package com.exoreaction.xorcery.reactivestreams.common;

import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This tracks active subscriptions to publishers or from subscribers exposed by this server
 */
public class ActiveSubscriptions {

    private final Set<ActiveSubscription> activeSubscriptions = new CopyOnWriteArraySet<>();

    public void addSubscription(ActiveSubscription activeSubscription) {
        activeSubscriptions.add(activeSubscription);
    }

    public void removeSubscription(ActiveSubscription activeSubscription) {
        if (activeSubscription != null)
        {
            activeSubscriptions.remove(activeSubscription);
        }
    }

    public Collection<ActiveSubscription> getActiveSubscriptions() {
        return activeSubscriptions;
    }

    public record ActiveSubscription(String stream, AtomicLong requested, AtomicLong received, Configuration subscriptionConfiguration) {
    }
}
