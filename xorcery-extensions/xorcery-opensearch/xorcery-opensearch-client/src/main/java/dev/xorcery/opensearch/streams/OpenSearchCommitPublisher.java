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
package dev.xorcery.opensearch.streams;

import dev.xorcery.metadata.WithMetadata;
import dev.xorcery.opensearch.api.IndexCommit;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Consumer;

public class OpenSearchCommitPublisher
        implements Publisher<WithMetadata<IndexCommit>>, Consumer<WithMetadata<IndexCommit>> {

    private static final Sinks.Many<WithMetadata<IndexCommit>> sink = Sinks.many().multicast().onBackpressureBuffer(4096, false);
    private static final Flux<WithMetadata<IndexCommit>> sinkPublisher = sink.asFlux();

    @Override
    public void subscribe(Subscriber<? super WithMetadata<IndexCommit>> subscriber) {
        sinkPublisher.subscribe(subscriber);
    }

    @Override
    public void accept(WithMetadata<IndexCommit> indexCommitWithMetadata) {
        sink.tryEmitNext(indexCommitWithMetadata);
    }
}
