package dev.xorcery.reactivestreams.server;

import dev.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.function.BiFunction;
import java.util.function.Function;

public record SubProtocolHandlers<INPUT, OUTPUT>(ReactiveStreamSubProtocol subProtocol,
                                                 Class<? super INPUT> readerType,
                                                 Class<? super OUTPUT> writerType,
                                                 Publisher<OUTPUT> publisher,
                                                 Function<Flux<INPUT>, Publisher<OUTPUT>> subscriberWithResult,
                                                 Function<Flux<INPUT>, Flux<INPUT>> subscriberTransform,
                                                 BiFunction<FluxSink<OUTPUT>, Flux<INPUT>, Flux<INPUT>> publisherWithResult) {
}
