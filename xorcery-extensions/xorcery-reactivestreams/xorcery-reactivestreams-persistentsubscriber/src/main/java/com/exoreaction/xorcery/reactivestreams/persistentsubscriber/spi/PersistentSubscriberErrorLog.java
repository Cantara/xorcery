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
package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.metadata.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jvnet.hk2.annotations.Contract;

import java.io.Closeable;
import java.io.IOException;

@Contract
public interface PersistentSubscriberErrorLog
        extends Closeable {

    void init(PersistentSubscriberConfiguration configuration, PersistentSubscriber persistentSubscriber) throws IOException;

    void handle(MetadataJsonNode<ArrayNode> metadataJsonNode, Throwable exception) throws IOException;
}
