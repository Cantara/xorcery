package dev.xorcery.opentelemetry.exporters.reactivestreams;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service(name = "opentelemetry.exporters.reactivestreams")
public class ReactiveStreamExporterService {

    private final Sinks.Many<MetadataJsonNode<JsonNode>> collector;

    @Inject
    public ReactiveStreamExporterService() {
        collector = Sinks.many().unicast().onBackpressureBuffer();
    }

    public void send(MetadataJsonNode<JsonNode> item) {
        collector.tryEmitNext(item);
    }

    public Flux<MetadataJsonNode<JsonNode>> getCollector() {
        return collector.asFlux();
    }
}
