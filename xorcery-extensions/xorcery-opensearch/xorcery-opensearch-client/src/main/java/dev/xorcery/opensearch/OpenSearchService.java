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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.opensearch.client.OpenSearchClient;
import dev.xorcery.opensearch.client.document.DocumentUpdates;
import dev.xorcery.opensearch.client.index.AcknowledgedResponse;
import dev.xorcery.opensearch.client.index.CreateComponentTemplateRequest;
import dev.xorcery.opensearch.client.index.CreateIndexTemplateRequest;
import dev.xorcery.opensearch.client.index.IndexTemplate;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service(name = "opensearch")
@RunLevel(4)
public class OpenSearchService
        implements PreDestroy {

    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;
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
        this.objectMapper = new ObjectMapper(new YAMLFactory());

        URI host = openSearchConfiguration.getURI();
        client = clientFactory.create(host);

        loadComponentTemplates();
        loadIndexTemplates();

        try {
            Map<String, IndexTemplate> templates = client.indices().getIndexTemplates().toCompletableFuture().get(10, TimeUnit.SECONDS).getIndexTemplates();
            for (String templateName : templates.keySet()) {
                logger.info("Index template:" + templateName);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public OpenSearchClient getClient() {
        return client;
    }

    public DocumentUpdates documentUpdates(Function<MetadataJsonNode<JsonNode>, String> documentIdSelector) {
        return new DocumentUpdates(openSearchConfiguration, clientFactory, documentIdSelector, logger, openTelemetry);
    }

    private void loadComponentTemplates() {
        // Upsert component templates
        List<OpenSearchConfiguration.Template> componentTemplates = openSearchConfiguration.getComponentTemplates();

        for (OpenSearchConfiguration.Template componentTemplate : componentTemplates) {
            String templateId = componentTemplate.getId();
            URI templateName = componentTemplate.getResource();
            logger.info("Loading component template '{}' from: ", templateId, templateName);
            try (InputStream in = templateName.toURL().openStream()) {
                String templateSource = new String(in.readAllBytes(), StandardCharsets.UTF_8);

                CreateComponentTemplateRequest request = new CreateComponentTemplateRequest.Builder((ObjectNode) objectMapper.readTree(templateSource)).build();

                AcknowledgedResponse response = client.indices().createComponentTemplate(templateId, request).get(10, TimeUnit.SECONDS);
                if (!response.isAcknowledged()) {
                    logger.error("Could not upload template " + templateName + ":\n" + response.json().toPrettyString());
                }
            } catch (Throwable ex) {
                logger.error("Could not load template " + templateName, ex);
            }
        }
    }

    private void loadIndexTemplates() {
        // Upsert index templates
        List<OpenSearchConfiguration.Template> indexTemplates = openSearchConfiguration.getIndexTemplates();

        for (OpenSearchConfiguration.Template indexTemplate : indexTemplates) {
            String templateId = indexTemplate.getId();
            URI templateName = indexTemplate.getResource();
            logger.info("Loading index template '{} from: {}", templateId, templateName);
            try (InputStream in = templateName.toURL().openStream()) {
                String templateSource = new String(in.readAllBytes(), StandardCharsets.UTF_8);

                CreateIndexTemplateRequest createIndexTemplateRequest = new CreateIndexTemplateRequest.Builder((ObjectNode) objectMapper.readTree(templateSource)).build();

                AcknowledgedResponse response = client.indices().createIndexTemplate(templateId, createIndexTemplateRequest).get(10, TimeUnit.SECONDS);
                if (!response.isAcknowledged()) {
                    logger.error("Could not load template " + templateName + ":\n" + response.json().toPrettyString());
                }
            } catch (Throwable ex) {
                logger.error("Could not load template " + templateName, ex);
            }
        }
    }

    @Override
    public void preDestroy() {
        if (openSearchConfiguration.isDeleteOnExit()) {
            // Delete standard indexes (useful for testing)
            try {

                {
                    AcknowledgedResponse deleteIndexResponse = client.indices().deleteIndex("domainevents-*")
                            .toCompletableFuture().get(10, TimeUnit.SECONDS);
                }

                {
                    AcknowledgedResponse deleteIndexResponse = client.indices().deleteIndex("metrics-*")
                            .toCompletableFuture().get(10, TimeUnit.SECONDS);
                }

                {
                    AcknowledgedResponse deleteIndexResponse = client.indices().deleteIndex("requestlogs-*")
                            .toCompletableFuture().get(10, TimeUnit.SECONDS);
                }

                {
                    AcknowledgedResponse deleteIndexResponse = client.indices().deleteIndex("logs-*")
                            .toCompletableFuture().get(10, TimeUnit.SECONDS);
                }

                logger.info("Deleted OpenSearch indices");
            } catch (Throwable e) {
                logger.warn("Could not close OpenSearch service", e);
            }
        }
    }

}
