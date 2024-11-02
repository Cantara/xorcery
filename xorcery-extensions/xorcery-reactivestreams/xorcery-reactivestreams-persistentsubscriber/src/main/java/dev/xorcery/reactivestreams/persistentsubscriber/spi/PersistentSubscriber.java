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
package dev.xorcery.reactivestreams.persistentsubscriber.spi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Contract
public interface PersistentSubscriber {

    default void init(PersistentSubscriberConfiguration subscriberConfiguration)
            throws IOException
    {
    }

    default Predicate<MetadataJsonNode<ArrayNode>> getFilter() {
        return wman -> true;
    }

    void handle(MetadataJsonNode<ArrayNode> metadataJson, CompletableFuture<Void> result);
}
