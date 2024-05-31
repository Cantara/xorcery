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

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.process.Process;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberErrorLog;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public record PersistentSubscriberProcess(
        PersistentSubscriberConfiguration configuration,
        ReactiveStreamsClient reactiveStreamsClient,
        PersistentSubscriber persistentSubscriber,
        PersistentSubscriberCheckpoint persistentSubscriberCheckpoint,
        PersistentSubscriberErrorLog persistentSubscriberErrorLog,
        Logger logger,
        CompletableFuture<Void> result)
        implements Process<Void> {

    @Override
    public void start() {
        URI eventsUri = configuration.getURI();
        String stream = configuration().getStream();

        logger.info("Starting persistent subscriber for {}#{}", eventsUri, stream);

        PersistentSubscriberSubscriber subscriber = new PersistentSubscriberSubscriber(
                this,
                configuration,
                persistentSubscriber,
                persistentSubscriberCheckpoint,
                persistentSubscriberErrorLog,
                logger);

        CompletableFuture<Void> subscribeFuture = reactiveStreamsClient.subscribe(eventsUri,
                stream,
                this::publisherStreamConfiguration,
                subscriber,
                subscriber.getClass(),
                ClientConfiguration.defaults());
        subscribeFuture.whenComplete(this::complete);

        result.whenComplete((r, t) ->
        {
            subscribeFuture.cancel(false);
        });
    }

    private Configuration publisherStreamConfiguration() {

        try {
            Configuration.Builder builder = configuration.getConfiguration().asBuilder();
            long revision = persistentSubscriberCheckpoint.getCheckpoint();
            builder.add("from", revision);
            return builder.build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean isRetryable(Throwable t) {
        if (t instanceof CancellationException)
            return false;

        logger.warn("Event processing error, retrying", t);

        return true;
    }

    @Override
    public void stop() {
        try {
            persistentSubscriberCheckpoint.close();
            persistentSubscriberErrorLog.close();
        } catch (IOException e) {
            logger.error("Could not stop subscriber process", e);
        }

        Process.super.stop();
    }
}
