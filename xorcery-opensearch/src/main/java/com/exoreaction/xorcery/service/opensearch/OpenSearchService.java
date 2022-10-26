package com.exoreaction.xorcery.service.opensearch;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.opensearch.api.OpenSearchRels;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.index.AcknowledgedResponse;
import com.exoreaction.xorcery.service.opensearch.client.index.CreateComponentTemplateRequest;
import com.exoreaction.xorcery.service.opensearch.client.index.CreateIndexTemplateRequest;
import com.exoreaction.xorcery.service.opensearch.client.index.IndexTemplate;
import com.exoreaction.xorcery.service.opensearch.streams.ClientSubscriberGroupListener;
import com.exoreaction.xorcery.service.opensearch.streams.OpenSearchCommitPublisher;
import com.exoreaction.xorcery.service.opensearch.streams.OpenSearchSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.client.ClientConfig;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Named(OpenSearchService.SERVICE_TYPE)
public class OpenSearchService
        implements PreDestroy {
    private static Logger logger = LogManager.getLogger(OpenSearchService.class);

    public static final String SERVICE_TYPE = "opensearch";
    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;
    private final Configuration configuration;

    @Inject
    public OpenSearchService(Topic<ServiceResourceObject> registryTopic,
                             ServiceLocator serviceLocator,
                             ReactiveStreams reactiveStreams,
                             Configuration configuration,
                             ClientConfig clientConfig) {

        ServiceResourceObject sro = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .websocket(OpenSearchRels.opensearch.name(), "ws/opensearch")
                .websocket(OpenSearchRels.opensearchcommits.name(), "ws/opensearchcommits")
                .build();

        this.configuration = configuration;
        this.objectMapper = new ObjectMapper(new YAMLFactory());

        URI host = configuration.getURI("opensearch.url").orElseThrow();
        client = new OpenSearchClient(clientConfig, host);

        loadComponentTemplates();
        loadIndexTemplates();

        try {
            Map<String, IndexTemplate> templates = client.indices().getIndexTemplates().toCompletableFuture().get(10, TimeUnit.SECONDS).getIndexTemplates();
            for (String templateName : templates.keySet()) {
                logger.info("Index template:" + templateName);
            }

            OpenSearchCommitPublisher openSearchCommitPublisher = new OpenSearchCommitPublisher();

            ServiceLocatorUtilities.addOneConstant(serviceLocator, new ClientSubscriberGroupListener(client,
                    reactiveStreams, openSearchCommitPublisher, sro.getServiceIdentifier()));

            sro.getLinkByRel(OpenSearchRels.opensearch.name()).ifPresent(link ->
            {
                reactiveStreams.subscriber(link.getHrefAsUri().getPath(), cfg -> new OpenSearchSubscriber(client, openSearchCommitPublisher, cfg), OpenSearchSubscriber.class);
            });

            sro.getLinkByRel(OpenSearchRels.opensearchcommits.name()).ifPresent(link ->
            {
                reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> {
                    return openSearchCommitPublisher;
                }, OpenSearchCommitPublisher.class);
            });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        registryTopic.publish(sro);
    }

    private void loadComponentTemplates() {
        // Upsert component templates
        JsonNode jsonNode = configuration.getJson("opensearch.component_templates").orElseThrow(() ->
                new IllegalStateException("Missing opensearch.component_templates configuration"));
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
        JsonNode jsonNode = configuration.getJson("opensearch.index_templates").orElseThrow(() ->
                new IllegalStateException("Missing opensearch.index_templates configuration"));
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
        if (configuration.getBoolean("opensearch.deleteonexit").orElse(false)) {
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
