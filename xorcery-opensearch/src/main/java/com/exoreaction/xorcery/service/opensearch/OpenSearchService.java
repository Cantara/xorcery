package com.exoreaction.xorcery.service.opensearch;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jersey.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.opensearch.api.OpenSearchRels;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.index.AcknowledgedResponse;
import com.exoreaction.xorcery.service.opensearch.client.index.CreateComponentTemplateRequest;
import com.exoreaction.xorcery.service.opensearch.client.index.CreateIndexTemplateRequest;
import com.exoreaction.xorcery.service.opensearch.client.index.IndexTemplate;
import com.exoreaction.xorcery.service.opensearch.streams.ClientSubscriberConductorListener;
import com.exoreaction.xorcery.service.opensearch.streams.OpenSearchCommitPublisher;
import com.exoreaction.xorcery.service.opensearch.streams.OpenSearchSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Singleton
public class OpenSearchService
        implements ContainerLifecycleListener, LifeCycle.Listener {
    private static Logger logger = LogManager.getLogger(OpenSearchService.class);

    public static final String SERVICE_TYPE = "opensearch";
    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;
    private Conductor conductor;
    private ReactiveStreams reactiveStreams;
    private ScheduledExecutorService scheduledExecutorService;
    private Configuration configuration;
    private JettyHttpClientContract instance;
    private ServiceResourceObject sro;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.websocket(OpenSearchRels.opensearch.name(), "ws/opensearch")
                    .websocket(OpenSearchRels.opensearchcommits.name(), "ws/opensearchcommits");
        }

        @Override
        protected void configure() {
            context.register(OpenSearchService.class, ContainerLifecycleListener.class);
        }
    }

    @Inject
    public OpenSearchService(Conductor conductor,
                             ReactiveStreams reactiveStreams,
                             Configuration configuration,
                             JettyHttpClientContract instance,
                             Server server,
                             @Named(SERVICE_TYPE) ServiceResourceObject sro) {
        this.conductor = conductor;
        this.reactiveStreams = reactiveStreams;
        this.configuration = configuration;
        this.instance = instance;
        this.sro = sro;
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        server.addEventListener(this);

        URI host = configuration.getURI("opensearch.url").orElseThrow();
        client = new OpenSearchClient(new ClientConfig()
                .register(new LoggingFeature.LoggingFeatureBuilder()
                        .level(Level.INFO)
                        .withLogger(java.util.logging.Logger.getLogger("client.opensearch")).build())
                .register(instance)
                .connectorProvider(new JettyConnectorProvider()), host);
    }

    @Override
    public void onStartup(Container container) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        loadComponentTemplates();
        loadIndexTemplates();

        try {
            Map<String, IndexTemplate> templates = client.indices().getIndexTemplates().toCompletableFuture().get(10, TimeUnit.SECONDS).getIndexTemplates();
            for (String templateName : templates.keySet()) {
                logger.info("Index template:" + templateName);
            }

            OpenSearchCommitPublisher openSearchCommitPublisher = new OpenSearchCommitPublisher();

            conductor.addConductorListener(new ClientSubscriberConductorListener(client,
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
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {

        if (configuration.getBoolean("opensearch.deleteonexit").orElse(false))
        {
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
