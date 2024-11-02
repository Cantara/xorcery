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
package dev.xorcery.reactivestreams.persistentsubscriber;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import dev.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import dev.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import dev.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberErrorLog;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

public class PersistentSubscriberSubscriber
        implements Function<Flux<MetadataJsonNode<ArrayNode>>, Publisher<MetadataJsonNode<ArrayNode>>> {
    private final PersistentSubscriberProcess persistentSubscriberProcess;
    private final PersistentSubscriberConfiguration configuration;
    private final PersistentSubscriber persistentSubscriber;
    private final Predicate<MetadataJsonNode<ArrayNode>> filter;
    private final PersistentSubscriberCheckpoint persistentSubscriberCheckpoint;
    private final PersistentSubscriberErrorLog persistentSubscriberErrorLog;
    private final Logger logger;

    long skipped = 0;

    public PersistentSubscriberSubscriber(
            PersistentSubscriberProcess persistentSubscriberProcess,
            PersistentSubscriberConfiguration configuration,
            PersistentSubscriber persistentSubscriber,
            PersistentSubscriberCheckpoint persistentSubscriberCheckpoint,
            PersistentSubscriberErrorLog persistentSubscriberErrorLog,
            Logger logger) {

        this.persistentSubscriberProcess = persistentSubscriberProcess;
        this.configuration = configuration;
        this.persistentSubscriber = persistentSubscriber;
        this.filter = persistentSubscriber.getFilter();
        this.persistentSubscriberCheckpoint = persistentSubscriberCheckpoint;
        this.persistentSubscriberErrorLog = persistentSubscriberErrorLog;
        this.logger = logger;
    }

    @Override
    public Publisher<MetadataJsonNode<ArrayNode>> apply(Flux<MetadataJsonNode<ArrayNode>> metadataJsonNodeFlux) {
        return metadataJsonNodeFlux.handle(this::handle);
    }

    private void handle(MetadataJsonNode<ArrayNode> arrayNodeWithMetadata, SynchronousSink<MetadataJsonNode<ArrayNode>> sink) {
        try {
            if (!filter.test(arrayNodeWithMetadata)) {
                skipped++;
                if (skipped == 256) {
                    skipped = 0;
                    long revision = arrayNodeWithMetadata.metadata().getLong("revision").orElseThrow(() -> new IllegalStateException("Metadata does not contain revision"));
                    persistentSubscriberCheckpoint.setCheckpoint(revision);
                }
                return;
            }

            long revision = arrayNodeWithMetadata.metadata().getLong("revision").orElseThrow(() -> new IllegalStateException("Metadata does not contain revision"));

            CompletableFuture<Void> future = new CompletableFuture<>();
            persistentSubscriber.handle(arrayNodeWithMetadata, future);

            try {
                future.orTimeout(30, TimeUnit.SECONDS).join();

                persistentSubscriberCheckpoint.setCheckpoint(revision);
            } catch (Throwable t) {
                persistentSubscriberErrorLog.handle(arrayNodeWithMetadata, t);
            }
            skipped = 0;
            sink.next(arrayNodeWithMetadata);
        } catch (Throwable e) {
            sink.error(e);
        }
    }
}
