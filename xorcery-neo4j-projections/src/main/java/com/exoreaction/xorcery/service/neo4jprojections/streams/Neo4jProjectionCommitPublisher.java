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
package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.exoreaction.xorcery.disruptor.handlers.BroadcastEventHandler;
import com.exoreaction.xorcery.service.neo4jprojections.api.ProjectionCommit;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class Neo4jProjectionCommitPublisher
        implements Flow.Publisher<WithMetadata<ProjectionCommit>>, Consumer<WithMetadata<ProjectionCommit>> {
    BroadcastEventHandler<WithMetadata<ProjectionCommit>> broadcastEventHandler = new BroadcastEventHandler<>(true);

    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<ProjectionCommit>> subscriber) {
        subscriber.onSubscribe(broadcastEventHandler.add(subscriber));
    }

    @Override
    public void accept(WithMetadata<ProjectionCommit> projectionCommitWithMetadata) {
        // TODO Replace with disruptor
        try {
            broadcastEventHandler.onEvent(projectionCommitWithMetadata, 0, true);
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Error publishing projection commit", e);
        }
    }
}
