package dev.xorcery.opentelemetry.exporters.websocket.listen;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.opentelemetry.exporters.websocket.WebsocketExporterService;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;

@Service(name="opentelemetry.exporters.websocket.listen")
@RunLevel(8)
public class ListenExporterService
        implements PreDestroy
{
    private final Disposable disposable;

    @Inject
    public ListenExporterService(WebsocketExporterService websocketExporterService, ServerWebSocketStreams serverWebSocketStreams, Configuration configuration, Logger logger)  {
        ListenExporterConfiguration exporterConfiguration = ListenExporterConfiguration.get(configuration);
        this.disposable = serverWebSocketStreams.publisher(exporterConfiguration.getPath(), MetadataByteBuffer.class, websocketExporterService.getCollector());
        logger.info("Listening at {}", exporterConfiguration.getUri());
    }

    @Override
    public void preDestroy() {
        disposable.dispose();
    }
}
