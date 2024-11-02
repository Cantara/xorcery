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
import dev.xorcery.opensearch.client.index.AcknowledgedResponse;
import dev.xorcery.opensearch.client.index.CreateComponentTemplateRequest;
import dev.xorcery.opensearch.client.index.CreateIndexTemplateRequest;
import dev.xorcery.opensearch.client.index.IndexTemplate;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service(name="opensearch")
public class OpenSearchService
        implements PreDestroy {

    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;
    private final OpenSearchConfiguration openSearchConfiguration;
    private final Logger logger;

    @Inject
    public OpenSearchService(Configuration configuration,
                             ClientBuilder clientBuilder,
                             Logger logger) {

        this.openSearchConfiguration = new OpenSearchConfiguration(configuration.getConfiguration("opensearch"));
        this.logger = logger;
        this.objectMapper = new ObjectMapper(new YAMLFactory());

        URI host = openSearchConfiguration.getURI();
        client = new OpenSearchClient(clientBuilder, host);

        loadComponentTemplates();
        loadIndexTemplates();

        try {
            Map<String, IndexTemplate> templates = client.indices().getIndexTemplates().toCompletableFuture().get(10, TimeUnit.SECONDS).getIndexTemplates();
            for (String templateName : templates.keySet()) {
                logger.info("Index template:" + templateName);
            }

            /** None of this makes sense. This service should only handle the client, not the streams
             OpenSearchCommitPublisher openSearchCommitPublisher = new OpenSearchCommitPublisher();

             reactiveStreamsServer.<WithMetadata<JsonNode>>subscriber("opensearch", MediaType.APPLICATION_JSON, (Class)WithMetadata.class, flux->
             {
             flux.publish().autoConnect().subscribe(new OpenSearchSubscriber(client, openSearchCommitPublisher, new Configuration.Builder().build(), new Configuration.Builder().build()));
             return flux;
             });
             Type publisherItemType = Classes.resolveActualTypeArgs(OpenSearchCommitPublisher.class, Publisher.class)[0];
             reactiveStreamsServer.publisher("opensearchcommits", MediaType.APPLICATION_JSON, publisherItemType, openSearchCommitPublisher);

             OpenSearchSubscriberConnector connector = new OpenSearchSubscriberConnector(client, reactiveStreamsClient, openSearchCommitPublisher);
             openSearchConfiguration.getPublishers().ifPresent(publishers ->
             {
             for (OpenSearchConfiguration.Publisher publisher : publishers) {
             connector.connect(publisher.getURI().orElse(null), publisher.getServerConfiguration().orElseGet(Configuration::empty), publisher.getClientConfiguration().orElseGet(Configuration::empty));
             }
             });
             **/
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public OpenSearchClient getClient() {
        return client;
    }

    private void loadComponentTemplates() {
        // Upsert component templates
        JsonNode jsonNode = openSearchConfiguration.getComponentTemplates();
        Iterator<String> fieldNames = jsonNode.fieldNames();

        while (fieldNames.hasNext()) {
            String templateId = fieldNames.next();
            String templateName = jsonNode.get(templateId).textValue();
            logger.info("Loading OpenSearch component template from:" + templateName);
            String templateSource = null;
            try {
                URI templateUri = URI.create(templateName);
                templateSource = Files.readString(Path.of(templateUri));
            } catch (IllegalArgumentException | IOException e) {
                // Just load from classpath
                try (InputStream in = ClassLoader.getSystemResourceAsStream(templateName)) {
                    if (in != null)
                        templateSource = new String(in.readAllBytes());
                } catch (IOException ex) {
                    logger.error("Could not load template " + templateName, ex);
                }
            }

            if (templateSource != null) {
                try {
                    CreateComponentTemplateRequest request = new CreateComponentTemplateRequest.Builder((ObjectNode) objectMapper.readTree(templateSource)).build();

                    AcknowledgedResponse response = client.indices().createComponentTemplate(templateId, request).toCompletableFuture().get(10, TimeUnit.SECONDS);
                    if (!response.isAcknowledged()) {
                        logger.error("Could not load template " + templateName + ":\n" + response.json().toPrettyString());
                    }
                } catch (Throwable e) {
                    logger.error("Could not load template " + templateName, e);
                }
            } else {
                logger.error("Could not find template " + templateName);
            }
        }
    }

    private void loadIndexTemplates() {
        // Upsert index templates
        JsonNode jsonNode = openSearchConfiguration.getIndexTemplates();
        Iterator<String> fieldNames = jsonNode.fieldNames();

        while (fieldNames.hasNext()) {
            String templateId = fieldNames.next();
            String templateName = jsonNode.get(templateId).textValue();
            logger.info("Loading OpenSearch index template from:" + templateName);
            String templateSource = null;
            try {
                URI templateUri = URI.create(templateName);
                templateSource = Files.readString(Path.of(templateUri));
            } catch (IllegalArgumentException | IOException e) {
                // Just load from classpath
                try (InputStream in = ClassLoader.getSystemResourceAsStream(templateName)) {
                    if (in != null)
                        templateSource = new String(in.readAllBytes());
                } catch (IOException ex) {
                    logger.error("Could not load template " + templateName, ex);
                }
            }

            if (templateSource != null) {
                try {
                    CreateIndexTemplateRequest createIndexTemplateRequest = new CreateIndexTemplateRequest.Builder((ObjectNode) objectMapper.readTree(templateSource)).build();

                    AcknowledgedResponse response = client.indices().createIndexTemplate(templateId, createIndexTemplateRequest).toCompletableFuture().get(10, TimeUnit.SECONDS);
                    if (!response.isAcknowledged()) {
                        logger.error("Could not load template " + templateName + ":\n" + response.json().toPrettyString());
                    }
                } catch (Throwable e) {
                    logger.error("Could not load template " + templateName, e);
                }
            } else {
                logger.error("Could not find template " + templateName);
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
