package dev.xorcery.domainevents.publisher.spi;

import dev.xorcery.metadata.Metadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

/**
 * An EventProjectionProvider waits for the streamPosition in Metadata to be projected, so that subscribers can know
 * when to expect the data to be available for querying.
 */
public interface EventProjectionProvider extends BiFunction<Flux<Metadata>, ContextView, Publisher<Metadata>> {
}
