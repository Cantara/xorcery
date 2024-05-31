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
package com.exoreaction.xorcery.reactivestreams.persistentsubscriber;

import com.exoreaction.xorcery.metadata.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberErrorLog;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class PersistentSubscriberSubscriber
        implements Subscriber<MetadataJsonNode<ArrayNode>> {
    private final PersistentSubscriberProcess persistentSubscriberProcess;
    private final PersistentSubscriberConfiguration configuration;
    private final PersistentSubscriber persistentSubscriber;
    private final Predicate<WithMetadata<ArrayNode>> filter;
    private final PersistentSubscriberCheckpoint persistentSubscriberCheckpoint;
    private final PersistentSubscriberErrorLog persistentSubscriberErrorLog;
    private final Logger logger;
    private Subscription subscription;

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
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        logger.debug("onSubscribe");
        subscription.request(1024);
    }

    @Override
    public void onNext(MetadataJsonNode<ArrayNode> arrayNodeWithMetadata) {

        try {
            if (!filter.test(arrayNodeWithMetadata)) {
                skipped++;
                if (skipped == 256) {
                    subscription.request(skipped);
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
            subscription.request(1 + skipped);
            skipped = 0;
        } catch (Throwable e) {
            subscription.cancel();
            logger.error("Persistent subscriber cancelled", e);
        }
    }

    @Override
    public void onError(Throwable t) {
        persistentSubscriberProcess.retry();
    }

    @Override
    public void onComplete() {
        logger.info("onComplete");
    }
}
