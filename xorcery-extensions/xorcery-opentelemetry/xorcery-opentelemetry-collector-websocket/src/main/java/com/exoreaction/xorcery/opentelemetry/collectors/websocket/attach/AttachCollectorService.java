package com.exoreaction.xorcery.opentelemetry.collectors.websocket.attach;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.opentelemetry.collectors.websocket.WebsocketCollectorService;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.util.context.Context;
import reactor.util.context.ContextView;
import reactor.util.retry.Retry;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service(name = "opentelemetry.collectors.websocket.attach")
@RunLevel(6)
public class AttachCollectorService
        implements PreDestroy {
    private final WebsocketCollectorService websocketCollectorService;
    private final ClientWebSocketStreams clientWebSocketStreams;
    private final AttachCollectorConfiguration attachCollectorConfiguration;
    private final Map<URI, Disposable> attached = new ConcurrentHashMap<>();

    @Inject
    public AttachCollectorService(WebsocketCollectorService websocketCollectorService, Configuration configuration, Logger logger, ClientWebSocketStreams clientWebSocketStreams) {
        attachCollectorConfiguration = AttachCollectorConfiguration.get(configuration);
        this.websocketCollectorService = websocketCollectorService;
        this.clientWebSocketStreams = clientWebSocketStreams;
    }

    public Disposable attach(URI exporterUri) {
        ContextView webSocketContext = Context.of(ClientWebSocketStreamContext.serverUri.name(), exporterUri);
        Disposable disposable = Disposables.composite(() -> attached.remove(exporterUri), clientWebSocketStreams.subscribe(ClientWebSocketOptions.instance(), MetadataByteBuffer.class)
                .contextWrite(webSocketContext)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, attachCollectorConfiguration.getMinimumBackoff()))
                .subscribe(websocketCollectorService::collect));
        attached.put(exporterUri, disposable);
        return disposable;
    }

    public Collection<URI> getAttached() {
        return attached.keySet();
    }

    @Override
    public void preDestroy() {
        new ArrayList<>(attached.values()).forEach(Disposable::dispose);
    }
}
