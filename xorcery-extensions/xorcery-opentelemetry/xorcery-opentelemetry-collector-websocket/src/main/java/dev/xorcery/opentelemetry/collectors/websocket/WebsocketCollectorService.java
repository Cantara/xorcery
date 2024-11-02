package dev.xorcery.opentelemetry.collectors.websocket;

import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Sinks;

@Service(name = "opentelemetry.collectors.websocket")
public class WebsocketCollectorService
    implements PreDestroy
{

    private final Sinks.Many<MetadataByteBuffer> logs;
    private final Sinks.Many<MetadataByteBuffer> traces;
    private final Sinks.Many<MetadataByteBuffer> metrics;

    public WebsocketCollectorService() {
        this.logs = Sinks.many().multicast().onBackpressureBuffer();
        this.traces = Sinks.many().multicast().onBackpressureBuffer();
        this.metrics = Sinks.many().multicast().onBackpressureBuffer();
    }

    public void subscribeLogs(Subscriber<MetadataByteBuffer> subscriber)
    {
        logs.asFlux().subscribe(subscriber);
    }

    public void subscribeTraces(Subscriber<MetadataByteBuffer> subscriber)
    {
        traces.asFlux().subscribe(subscriber);
    }

    public void subscribeMetrics(Subscriber<MetadataByteBuffer> subscriber)
    {
        metrics.asFlux().subscribe(subscriber);
    }

    public void collect(MetadataByteBuffer metadataByteBuffer) {
        metadataByteBuffer.metadata().getString("type").ifPresent(type ->
        {
            switch (type)
            {
                case "log"-> logs.tryEmitNext(metadataByteBuffer);
                case "trace"-> traces.tryEmitNext(metadataByteBuffer);
                case "metric"-> metrics.tryEmitNext(metadataByteBuffer);
            }
        });
    }

    @Override
    public void preDestroy() {
        logs.tryEmitComplete();
        traces.tryEmitComplete();
        metrics.tryEmitComplete();
    }
}
