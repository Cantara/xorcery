package dev.xorcery.opentelemetry.exporters.websocket;

import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service(name = "opentelemetry.exporters.websocket")
public class WebsocketExporterService {

    private final Sinks.Many<MetadataByteBuffer> collector;

    @Inject
    public WebsocketExporterService() {
        collector = Sinks.many().unicast().onBackpressureBuffer();
    }

    public void send(MetadataByteBuffer metadataByteBuffer) {
        collector.tryEmitNext(metadataByteBuffer);
    }

    public Flux<MetadataByteBuffer> getCollector() {
        return collector.asFlux();
    }
}
