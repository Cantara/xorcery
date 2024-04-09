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
package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.neo4jprojections.api.ProjectionCommit;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Consumer;

public class Neo4jProjectionCommitPublisher
        implements Publisher<MetadataProjectionCommit>, Consumer<MetadataProjectionCommit> {

    private static final Sinks.Many<MetadataProjectionCommit> sink = Sinks.many().multicast().onBackpressureBuffer(4096, false);
    private static final Flux<MetadataProjectionCommit> sinkPublisher = sink.asFlux();

    @Override
    public void subscribe(Subscriber<? super MetadataProjectionCommit> subscriber) {
        sinkPublisher.subscribe(subscriber);
    }

    @Override
    public void accept(MetadataProjectionCommit projectionCommitWithMetadata) {
        sink.tryEmitNext(projectionCommitWithMetadata);
    }
}
