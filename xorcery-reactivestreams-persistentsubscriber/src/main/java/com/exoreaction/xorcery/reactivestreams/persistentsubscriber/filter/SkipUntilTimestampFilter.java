package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.filter;

import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.function.Predicate;

/**
 * Filter for when you don't want to process old events, and just want to skip everything up until a specified timestamp.
 *
 * @param timestampInclusive
 */
public record SkipUntilTimestampFilter(long timestampInclusive)
    implements Predicate<WithMetadata<ArrayNode>>
{
    @Override
    public boolean test(WithMetadata<ArrayNode> arrayNodeWithMetadata) {
        CommonMetadata commonMetadata = arrayNodeWithMetadata::metadata;
        return commonMetadata.getTimestamp() >= timestampInclusive;
    }
}
