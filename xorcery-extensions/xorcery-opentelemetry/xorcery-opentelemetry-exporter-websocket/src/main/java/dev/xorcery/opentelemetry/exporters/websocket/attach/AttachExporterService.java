package dev.xorcery.opentelemetry.exporters.websocket.attach;


import dev.xorcery.configuration.Configuration;
import dev.xorcery.opentelemetry.exporters.websocket.WebsocketExporterService;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;
import reactor.util.context.Context;
import reactor.util.context.ContextView;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service(name="opentelemetry.exporters.websocket.attach")
@RunLevel(8)
public class AttachExporterService
    implements PreDestroy
{
    private final Disposable disposable;
    private final AtomicBoolean resourceSent = new AtomicBoolean();
    private final String resourceId = UUID.randomUUID().toString();

    @Inject
    public AttachExporterService(WebsocketExporterService websocketExporterService, ClientWebSocketStreams clientWebSocketStreams, Configuration configuration, Logger logger)  {

        // Attach to collector
        AttachExporterConfiguration attachExporterConfiguration = AttachExporterConfiguration.get(configuration);
        URI collectorUri = attachExporterConfiguration.getCollectorUri();
        ContextView webSocketContext = Context.of(ClientWebSocketStreamContext.serverUri.name(), collectorUri);

        this.disposable = websocketExporterService.getCollector()
                .as(flux -> attachExporterConfiguration.isOptimizeResource() ? flux.doOnNext(this::optimizeResource) : flux)
                .transform(clientWebSocketStreams.publish(ClientWebSocketOptions.instance(), MetadataByteBuffer.class))
                .contextWrite(webSocketContext)
                .doOnError(throwable ->
                {
                    logger.error(String.format("Attach to %s failed", collectorUri), throwable);
                    resourceSent.set(false);
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10)))
                .subscribe();
        logger.info("Exporting to " + collectorUri);
    }

    private void optimizeResource(MetadataByteBuffer metadataByteBuffer) {
        metadataByteBuffer.metadata().json().put("resourceId", resourceId);
        if (resourceSent.get())
        {
            metadataByteBuffer.metadata().json().remove("resource");
        } else
        {
            resourceSent.set(true);
        }
    }

    @Override
    public void preDestroy() {
        disposable.dispose();
    }
}
