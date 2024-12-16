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
package dev.xorcery.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.opensearch.client.OpenSearchClient;
import dev.xorcery.opensearch.client.document.DocumentUpdates;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.net.URI;
import java.util.function.Function;

@Service(name = "opensearch")
@RunLevel(4)
public class OpenSearchService {

    private final OpenSearchClient client;
    private final OpenSearchConfiguration openSearchConfiguration;
    private final OpenSearchClient.Factory clientFactory;
    private final Logger logger;
    private final OpenTelemetry openTelemetry;

    @Inject
    public OpenSearchService(Configuration configuration,
                             OpenSearchClient.Factory clientFactory,
                             LoggerContext loggerContext,
                             OpenTelemetry openTelemetry) {

        this.openSearchConfiguration = OpenSearchConfiguration.get(configuration);
        this.clientFactory = clientFactory;
        this.logger = loggerContext.getLogger(getClass());
        this.openTelemetry = openTelemetry;
        URI host = openSearchConfiguration.getURI();
        client = clientFactory.create(host);
    }

    public OpenSearchClient getClient() {
        return client;
    }

    public DocumentUpdates documentUpdates(Function<MetadataJsonNode<JsonNode>, String> documentIdSelector, Function<ObjectNode, ObjectNode> jsonSelector) {
        return new DocumentUpdates(openSearchConfiguration, clientFactory, documentIdSelector, jsonSelector, logger, openTelemetry);
    }
}
