package dev.xorcery.eventstore.client;

import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ContextMetadataHandler
        implements BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>> {
    @Override
    public Publisher<MetadataByteBuffer> apply(Flux<MetadataByteBuffer> metadataByteBufferFlux, ContextView contextView) {
        return metadataByteBufferFlux.map(addContextAsMetadata(contextView));
    }

    private Function<? super MetadataByteBuffer, ? extends MetadataByteBuffer> addContextAsMetadata(ContextView contextView) {
        return metadataByteBuffer ->
        {
            Metadata.Builder builder = metadataByteBuffer.metadata().toBuilder();
            contextView.forEach((k, v) -> {
                // TODO Handle more types of values?
                if (v instanceof String str) {
                    builder.add(k.toString(), str);
                } else if (v instanceof Long nr) {
                    builder.add(k.toString(), nr);
                }
            });
            return metadataByteBuffer;
        };
    }
}
