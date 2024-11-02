package dev.xorcery.opentelemetry.collectors.websocket.listen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.opentelemetry.collectors.websocket.WebsocketCollectorService;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service(name = "opentelemetry.collectors.websocket.listen")
@RunLevel(6)
public class ListenCollectorService
    implements PreDestroy
{
    private final WebsocketCollectorService websocketCollectorService;
    private final Logger logger;
    private final ListenCollectorConfiguration configuration;
    private final Disposable disposable;
    private final Map<String, JsonNode> resourceIdToResource = new ConcurrentHashMap<>();

    @Inject
    public ListenCollectorService(WebsocketCollectorService websocketCollectorService, Configuration configuration, Logger logger, ServerWebSocketStreams serverWebSocketStreams) {
        this.websocketCollectorService = websocketCollectorService;
        this.logger = logger;
        this.configuration = ListenCollectorConfiguration.get(configuration);

        this.disposable = serverWebSocketStreams.subscriber(this.configuration.getPath(), MetadataByteBuffer.class, this::subscribe);
        logger.info("Collector listening at "+this.configuration.getPath());

    }

    private Publisher<MetadataByteBuffer> subscribe(Flux<MetadataByteBuffer> attachFlux) {
        return attachFlux
                .transformDeferredContextual((flux, context)->
                {
                    logger.info("Collecting from "+new ContextViewElement(context).<UpgradeRequest>get("request").map(request -> request.getUserPrincipal()).map(Principal::getName).orElse("unknown client"));
                    return flux;
                })
                .doOnNext(this::resourceIdMapping)
                .doOnNext(websocketCollectorService::collect);
    }

    private void resourceIdMapping(MetadataByteBuffer metadataByteBuffer) {
        metadataByteBuffer.metadata().getObjectAs("resource", Function.identity()).ifPresentOrElse(resource ->
        {
            metadataByteBuffer.metadata().getString("resourceId").ifPresent(resourceId ->
            {
                resourceIdToResource.put(resourceId, resource);
            });
        }, ()->
        {
            metadataByteBuffer.metadata().getString("resourceId").ifPresent(resourceId ->
            {
                if (resourceIdToResource.get(resourceId) instanceof ObjectNode resourceJson)
                {
                    metadataByteBuffer.metadata().json().put("resource", resourceJson);
                }
            });
        });
    }

    @Override
    public void preDestroy() {
        disposable.dispose();
    }
}
