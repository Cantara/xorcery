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
package dev.xorcery.reactivestreams.persistentsubscriber.filter;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.xorcery.metadata.CommonMetadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;

import java.util.function.Predicate;

/**
 * Filter for when you don't want to process old events, and just want to skip everything up until a specified timestamp.
 *
 * @param timestampInclusive
 */
public record SkipUntilTimestampFilter(long timestampInclusive)
    implements Predicate<MetadataJsonNode<ArrayNode>>
{
    @Override
    public boolean test(MetadataJsonNode<ArrayNode> arrayNodeWithMetadata) {
        CommonMetadata commonMetadata = arrayNodeWithMetadata::metadata;
        return commonMetadata.getTimestamp() >= timestampInclusive;
    }
}
