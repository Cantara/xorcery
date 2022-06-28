package com.exoreaction.xorcery.service.opensearch;

import com.exoreaction.xorcery.util.Listeners;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.opensearch.eventstore.EventStoreConductorListener;
import com.exoreaction.xorcery.service.opensearch.eventstore.domainevents.OpenSearchProjections;
import com.exoreaction.xorcery.service.opensearch.eventstore.domainevents.ProjectionListener;
import com.exoreaction.xorcery.service.opensearch.logging.LoggingConductorListener;
import com.exoreaction.xorcery.service.opensearch.metrics.MetricsConductorListener;
import com.exoreaction.xorcery.service.opensearch.requestlog.logging.RequestLogConductorListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.GetIndexTemplatesRequest;
import org.opensearch.client.indices.IndexTemplateMetadata;
import org.opensearch.client.indices.PutIndexTemplateRequest;
import org.opensearch.common.io.stream.OutputStreamStreamOutput;
import org.opensearch.common.xcontent.XContentType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
public class OpenSearchService
        implements ContainerLifecycleListener, OpenSearchProjections {
    private static Logger logger = LogManager.getLogger(OpenSearchService.class);

    public static final String SERVICE_TYPE = "opensearch";
    private final RestHighLevelClient client;
    private Conductor conductor;
    private ReactiveStreams reactiveStreams;
    private ScheduledExecutorService scheduledExecutorService;
    private Configuration configuration;
    private ServiceResourceObject sro;

    private Listeners<ProjectionListener> listeners = new Listeners<>(ProjectionListener.class);

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {

        }

        @Override
        protected void configure() {
            context.register(OpenSearchService.class, ContainerLifecycleListener.class, OpenSearchProjections.class);
        }
    }

    @Inject
    public OpenSearchService(Conductor conductor,
                             ReactiveStreams reactiveStreams,
                             Configuration configuration,
                             @Named(SERVICE_TYPE) ServiceResourceObject sro) {
        this.conductor = conductor;
        this.reactiveStreams = reactiveStreams;
        this.configuration = configuration;
        this.sro = sro;

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("admin", "admin"));

        URI host = configuration.getURI("opensearch.url").orElseThrow();
        RestClientBuilder builder = RestClient.builder(new HttpHost(host.getHost(), host.getPort(), host.getScheme()))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        client = new RestHighLevelClient(builder);
    }

    @Override
    public void onStartup(Container container) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        // Upsert index templates
        JsonNode jsonNode = configuration.getJson("opensearch.templates").orElseThrow(() ->
                new IllegalStateException("Missing opensearch.templates configuration"));
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
                try (InputStream in = getClass().getResourceAsStream(templateName)) {
                    if (in != null)
                        templateSource = new String(in.readAllBytes());
                } catch (IOException ex) {
                    logger.error("Could not load template " + templateName, ex);
                }
            }

            if (templateSource != null) {
                try {
                    PutIndexTemplateRequest putIndexTemplateRequest = new PutIndexTemplateRequest(templateId);
                    putIndexTemplateRequest.source(templateSource, XContentType.JSON);
                    AcknowledgedResponse response = client.indices().putTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT);
                    if (!response.isAcknowledged()) {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        response.writeTo(new OutputStreamStreamOutput(bout));
                        logger.error("Could not load template " + templateName + ":\n" + bout.toString());
                    }
                } catch (IOException e) {
                    logger.error("Could not load template " + templateName, e);
                }
            }
        }

        try {
            List<IndexTemplateMetadata> templates = client.indices().getIndexTemplate(new GetIndexTemplatesRequest(), RequestOptions.DEFAULT).getIndexTemplates();
            for (IndexTemplateMetadata template : templates) {
                logger.info("Template:"+template.name());
            }
        } catch (IOException e) {
            logger.error("Error listing templates", e);
        }
        conductor.addConductorListener(new LoggingConductorListener(client, reactiveStreams, sro.serviceIdentifier(), "logevents"));
        conductor.addConductorListener(new MetricsConductorListener(client, reactiveStreams, scheduledExecutorService, sro.serviceIdentifier(), "metricevents"));
        conductor.addConductorListener(new EventStoreConductorListener(client, reactiveStreams, sro.serviceIdentifier(), "eventstorestreams", listeners));
        conductor.addConductorListener(new RequestLogConductorListener(client, reactiveStreams, sro.serviceIdentifier(), "requestlogevents"));
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        try {

            {
                // Delete indexes for now
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("domainevents-*"); //Index name.
                AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            }

            {
                // Delete indexes for now
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("metrics-*"); //Index name.
                AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            }

            {
                // Delete indexes for now
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("logging-*"); //Index name.
                AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            }

            {
                // Delete indexes for now
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("requests-*"); //Index name.
                AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            }

            client.close();
        } catch (IOException e) {
            logger.warn("Could not close OpenSearch client", e);
        }
    }

    public void addProjectionListener(ProjectionListener listener)
    {
        listeners.addListener(listener);
    }

}
