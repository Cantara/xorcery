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
package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletableFuture;

@Service(name="testerrorsubscriber")
@RunLevel(19)
public class TestErrorSubscriber
    implements PersistentSubscriber
{
    @Inject
    public TestErrorSubscriber(Logger logger) {
        logger.info("Test error subscriber started");
    }

    @Override
    public void handle(MetadataJsonNode<ArrayNode> eventsWithMetadata, CompletableFuture<Void> result) {
        System.out.println(eventsWithMetadata.metadata().json().get("revision"));
        result.completeExceptionally(new IllegalArgumentException("Something went wrong"));
    }
}
