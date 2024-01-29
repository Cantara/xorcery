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

import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.filter.SkipUntilTimestampFilter;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers.BasePersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Service(name="testsubscriber")
@RunLevel(19)
public class TestSubscriber
    extends BasePersistentSubscriber
{
    public static AtomicLong handled = new AtomicLong();

    @Inject
    public TestSubscriber(Logger logger) {
        logger.info("Test subscriber started");
        handled.set(0);
    }

    @Override
    public void handle(WithMetadata<ArrayNode> eventsWithMetadata, CompletableFuture<Void> result) {
        System.out.println(eventsWithMetadata.metadata().json().get("revision"));
        handled.incrementAndGet();
        result.complete(null);
    }
}
