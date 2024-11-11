package dev.xorcery.domainevents.publisher.spi;

import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.metadata.Metadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

public interface EventPublisherProvider extends BiFunction<Flux<MetadataEvents>, ContextView, Publisher<Metadata>> {
}
