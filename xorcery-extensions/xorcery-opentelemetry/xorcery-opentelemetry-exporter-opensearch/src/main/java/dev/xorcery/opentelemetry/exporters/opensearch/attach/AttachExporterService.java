/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.opentelemetry.exporters.opensearch.attach;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.opensearch.OpenSearchService;
import dev.xorcery.opensearch.client.OpenSearchContext;
import dev.xorcery.opentelemetry.exporters.reactivestreams.ReactiveStreamExporterService;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.util.UUIDs;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;
import reactor.util.context.Context;
import reactor.util.context.ContextView;
import reactor.util.retry.Retry;

import java.net.URL;
import java.time.Duration;

@Service(name="opentelemetry.exporters.opensearch.attach")
@RunLevel(8)
public class AttachExporterService
    implements PreDestroy
{
    private final Disposable disposable;

    @Inject
    public AttachExporterService(ReactiveStreamExporterService reactiveStreamExporterService, OpenSearchService openSearchService, Configuration configuration, Logger logger)  {

        // Attach to collector
        AttachExporterConfiguration attachExporterConfiguration = AttachExporterConfiguration.get(configuration);
        URL hostUrl = attachExporterConfiguration.getHost();
        ContextView openSearchContext = Context.of(OpenSearchContext.serverUri.name(), hostUrl.toExternalForm());

        this.disposable = reactiveStreamExporterService.getCollector()
                .<MetadataJsonNode<JsonNode>>handle((item, sink) ->
                {
                    String index = switch (item.metadata().getString("type").orElse("unknown"))
                    {
                        case "log" -> "logs";
                        case "metric" -> "ss4o_metrics-otel";
                        case "trace" -> "traces";
                        default -> null;
                    };
                    if (index != null)
                    {
                        item.metadata().metadata().set("index", JsonNodeFactory.instance.textNode(index));
                        sink.next(item);
                    }
                })
                .transformDeferredContextual(openSearchService.documentUpdates(item -> UUIDs.newId(), json -> (ObjectNode)json.path("data")))
                .contextWrite(openSearchContext)
                .doOnError(throwable ->
                {
                    logger.error(String.format("Attach to %s failed", hostUrl), throwable);
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10)))
                .subscribe();
        logger.info("Exporting to " + hostUrl);
    }

    @Override
    public void preDestroy() {
        disposable.dispose();
    }
}
