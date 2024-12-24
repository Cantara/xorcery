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
package dev.xorcery.opensearch.templates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.opensearch.OpenSearchConfiguration;
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
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service(name = "opensearch.templates")
@RunLevel(4)
public class OpenSearchTemplatesService {

    private final OpenSearchClient client;
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;
    private final OpenSearchConfiguration openSearchConfiguration;
    private final OpenSearchTemplatesConfiguration openSearchTemplatesConfiguration;
    private final OpenSearchClient.Factory clientFactory;
    private final Logger logger;
    private final OpenTelemetry openTelemetry;

    @Inject
    public OpenSearchTemplatesService(Configuration configuration,
                                      OpenSearchClient.Factory clientFactory,
                                      LoggerContext loggerContext,
                                      OpenTelemetry openTelemetry) {

        this.openSearchConfiguration = OpenSearchConfiguration.get(configuration);
        this.openSearchTemplatesConfiguration = OpenSearchTemplatesConfiguration.get(configuration);
        this.clientFactory = clientFactory;
        this.logger = loggerContext.getLogger(getClass());
        this.openTelemetry = openTelemetry;
        this.yamlMapper = new YAMLMapper();
        this.jsonMapper = new JsonMapper();

        URI host = openSearchConfiguration.getURI();
        client = clientFactory.create(host);

        loadComponentTemplates();
        loadIndexTemplates();

        try {
            Map<String, IndexTemplate> templates = client.indices().getIndexTemplates().get(10, TimeUnit.SECONDS).getIndexTemplates();
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

    public DocumentUpdates documentUpdates(Function<MetadataJsonNode<JsonNode>, String> documentIdSelector, Function<ObjectNode, ObjectNode> jsonSelector) {
        return new DocumentUpdates(openSearchConfiguration, clientFactory, documentIdSelector, jsonSelector, logger, openTelemetry);
    }

    private void loadComponentTemplates() {
        // Upsert component templates
        List<OpenSearchTemplatesConfiguration.Template> componentTemplates = openSearchTemplatesConfiguration.getComponentTemplates();

        for (OpenSearchTemplatesConfiguration.Template componentTemplate : componentTemplates) {
            String templateId = componentTemplate.getId();
            URI templateName = componentTemplate.getResource();
            logger.info("Loading component template '{}' from:{}", templateId, templateName);
            try (InputStream in = templateName.toURL().openStream()) {
                String templateSource = new String(in.readAllBytes(), StandardCharsets.UTF_8);

                CreateComponentTemplateRequest request = new CreateComponentTemplateRequest.Builder((ObjectNode) yamlMapper.readTree(templateSource)).build();

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
        List<OpenSearchTemplatesConfiguration.Template> indexTemplates = openSearchTemplatesConfiguration.getIndexTemplates();

        for (OpenSearchTemplatesConfiguration.Template indexTemplate : indexTemplates) {
            String templateId = indexTemplate.getId();
            URI templateName = indexTemplate.getResource();
            logger.info("Loading index template '{}' from: {}", templateId, templateName);
            try (InputStream in = templateName.toURL().openStream()) {
                String templateSource = new String(in.readAllBytes(), StandardCharsets.UTF_8);

                ObjectNode templateJson = (ObjectNode) (templateName.getPath().endsWith("json")
                        ? jsonMapper.readTree(templateSource)
                        : yamlMapper.readTree(templateSource));
                CreateIndexTemplateRequest createIndexTemplateRequest = new CreateIndexTemplateRequest.Builder(templateJson).build();

                AcknowledgedResponse response = client.indices().createIndexTemplate(templateId, createIndexTemplateRequest).get(10, TimeUnit.SECONDS);
                if (!response.isAcknowledged()) {
                    logger.error("Could not load template " + templateName + ":\n" + response.json().toPrettyString());
                }
            } catch (Throwable ex) {
                logger.error("Could not load template " + templateName, ex);
            }
        }
    }
}
