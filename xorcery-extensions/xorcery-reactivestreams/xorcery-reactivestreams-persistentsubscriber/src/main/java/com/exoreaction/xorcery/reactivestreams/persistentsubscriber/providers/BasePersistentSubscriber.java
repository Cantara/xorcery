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
package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers;

import com.exoreaction.xorcery.metadata.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.filter.SkipUntilTimestampFilter;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Base implementation for PersistentSubscriber that helps set up useful event filters.
 */
public abstract class BasePersistentSubscriber
        implements PersistentSubscriber {

    protected Predicate<WithMetadata<ArrayNode>> filter;

    @Override
    public void init(PersistentSubscriberConfiguration subscriberConfiguration) {
        BasePersistentSubscriberConfiguration cfg = new BasePersistentSubscriberConfiguration(subscriberConfiguration.getConfiguration());
        Predicate<WithMetadata<ArrayNode>> filter = wman -> true;

        if (cfg.getSkipOld()) {
            filter = new SkipUntilTimestampFilter(System.currentTimeMillis());
        }

        Optional<Long> skipUntil = cfg.getSkipUntil();
        if (skipUntil.isPresent()) {
            filter = new SkipUntilTimestampFilter(skipUntil.get());
        }

        this.filter = filter;
    }

    @Override
    public Predicate<WithMetadata<ArrayNode>> getFilter() {
        return filter;
    }
}
