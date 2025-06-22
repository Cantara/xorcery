package dev.xorcery.kurrent.websocket.spi;

import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

public interface WriteTransformerProvider
        extends BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>>
{
}
