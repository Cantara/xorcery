package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers;

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
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
