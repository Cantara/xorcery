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
package dev.xorcery.neo4jprojections.api;

import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.neo4jprojections.Neo4jProjectionHandler;
import dev.xorcery.neo4jprojections.ProjectionModel;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;
import reactor.core.publisher.Sinks;
import reactor.util.context.ContextView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@Service
public class Neo4jProjections
        implements PreDestroy {
    private final Neo4jProjectionHandler projectionHandler;

    private final Map<String, SinkFlux> projectionSinks = new ConcurrentHashMap<>();
    private final Sinks.Many<String> projectionIdSink = Sinks.many().replay().all();

    @Inject
    public Neo4jProjections(Neo4jProjectionHandler neo4jProjectionHandler) {
        this.projectionHandler = neo4jProjectionHandler;

        projectionHandler.getProjections().forEach(projection->{
            projectionSinks.put(projection.getProjectionId(), createSinkFlux(projection.getProjectionId()));
        });
    }

    /**
     * To be used with {@link Flux#transformDeferred(Function)}.
     * Subscribers must place {@link ProjectionStreamContext#projectionId} into their {@link CoreSubscriber#currentContext()}.
     * The projection will add {@link ProjectionStreamContext#projectionPosition} to the context if the projection already exists.
     *
     * @return
     */
    public BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>> projection() {
        return projectionHandler.andThen(publisher ->
                Flux.from(publisher)
                        .transformDeferredContextual(this::updates));
    }

    // Current state
    public Optional<ProjectionModel> getProjection(String projectionId) {
        return projectionHandler.getProjection(projectionId);
    }

    public List<ProjectionModel> getProjections() {
        return projectionHandler.getProjections();
    }

    // Updates
    public Publisher<MetadataEvents> projectionUpdates(){
        return subscriber -> {
            if (subscriber instanceof CoreSubscriber<? super MetadataEvents> coreSubscriber) {
                new ContextViewElement(coreSubscriber.currentContext())
                        .getString(ProjectionStreamContext.projectionId)
                        .ifPresentOrElse(pid ->
                        {
                            // Subscribe to particular projection
                            projectionSinks.computeIfAbsent(pid, this::createSinkFlux)
                                    .flux.subscribe(subscriber);
                        }, () ->
                        {
                            subscriber.onError(new IllegalArgumentException("Must provide projectionId context key"));
                        });
            } else {
                subscriber.onError(new IllegalArgumentException("Must implement CoreSubscriber"));
            }
        };
    }

    public Publisher<String> projectionIds()
    {
        return subscriber->{
            projectionIdSink.asFlux().subscribe(subscriber);
        };
    }

    private Publisher<MetadataEvents> updates(Flux<MetadataEvents> projectionFlux, ContextView contextView) {
        return new ContextViewElement(contextView).getString(ProjectionStreamContext.projectionId).map(pid ->
        {
            SinkFlux sinkFlux = projectionSinks.computeIfAbsent(pid, this::createSinkFlux);
            return projectionFlux.doOnEach(onEach(sinkFlux.sink(), pid));
        }).orElse(projectionFlux);
    }

    private Consumer<Signal<MetadataEvents>> onEach(Sinks.Many<MetadataEvents> sink, String pid) {
        return signal ->
        {
            switch (requireNonNull(signal.getType())) {
                case CANCEL -> {
                    sink.tryEmitComplete();
                    projectionSinks.remove(pid);
                }
                case ON_NEXT -> {
                    sink.tryEmitNext(requireNonNull(signal.get()));
                }
                case ON_ERROR -> {
                    sink.tryEmitError(requireNonNull(signal.getThrowable()));
                    projectionSinks.remove(pid);
                }
                case ON_COMPLETE -> {
                    sink.tryEmitComplete();
                    projectionSinks.remove(pid);
                }
                default -> {
                }
            }
        };
    }

    private SinkFlux createSinkFlux(String projectionId) {
        projectionIdSink.tryEmitNext(projectionId);
        Sinks.Many<MetadataEvents> sink = Sinks.many().replay().limit(1);
        Flux<MetadataEvents> projectionUpdatesFlux = sink.asFlux();
        return new SinkFlux(sink, projectionUpdatesFlux);
    }

    @Override
    public void preDestroy() {
        projectionIdSink.tryEmitComplete();
    }

    record SinkFlux(Sinks.Many<MetadataEvents> sink, Flux<MetadataEvents> flux) {
    }
}
