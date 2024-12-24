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
package dev.xorcery.domainevents.neo4jprojection.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.domainevents.publisher.spi.EventProjectionProvider;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.neo4jprojections.api.Neo4jProjections;
import dev.xorcery.neo4jprojections.api.WaitForProjectionUpdate;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.context.ContextView;

import static dev.xorcery.domainevents.api.DomainEventMetadata.timestamp;
import static dev.xorcery.metadata.Metadata.missing;

@Service
@ContractsProvided(EventProjectionProvider.class)
public class Neo4jEventProjectionProvider
    implements EventProjectionProvider
{
    private final Neo4jProjections neo4jProjections;
    private final Configuration configuration;

    @Inject
    public Neo4jEventProjectionProvider(Neo4jProjections neo4jProjections, Configuration configuration) {
        this.neo4jProjections = neo4jProjections;
        this.configuration = configuration;
    }

    @Override
    public Publisher<Metadata> apply(Flux<Metadata> metadataFlux, ContextView contextView) {
        Sinks.Many<Metadata> sink = Sinks.many().unicast().onBackpressureBuffer();
        return configuration.getString("neo4jeventprojectionprovider.projectionId").map(pid ->
        {
            WaitForProjectionUpdate waiter = new WaitForProjectionUpdate(pid);
            Flux.from(neo4jProjections.projectionUpdates()).doOnComplete(sink::tryEmitComplete).subscribe(waiter);
            Disposable disposable = metadataFlux.subscribe(metadata ->
            {
                waiter.waitForTimestamp(metadata.getLong(timestamp).orElseThrow(missing(timestamp)))
                        .whenComplete((md, throwable)-> sink.tryEmitNext(metadata));
            });
            return sink.asFlux();
        }).orElseGet(()->Flux.error(new IllegalStateException("Missing projectionId configuration")));
    }
}
