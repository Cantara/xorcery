package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers;

import com.exoreaction.xorcery.configuration.Configuration;
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

    public record BasePersistentSubscriberConfiguration(Configuration configuration) {
        // if true in configuration, skip until System.currentTimeMillis()
        public boolean getSkipOld() {
            return configuration.getBoolean("skipOld").orElse(false);
        }

        // if set to long timestamp, skip until this timestamp is seen in event metadata
        public Optional<Long> getSkipUntil() {
            return configuration.getLong("skipUntil");
        }
    }

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
