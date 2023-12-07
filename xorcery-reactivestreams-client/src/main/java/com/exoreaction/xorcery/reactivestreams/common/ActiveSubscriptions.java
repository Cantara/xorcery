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
