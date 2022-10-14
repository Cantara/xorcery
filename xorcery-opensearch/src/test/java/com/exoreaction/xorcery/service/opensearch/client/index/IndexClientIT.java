package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@Disabled
public class IndexClientIT {

    private Logger logger = LogManager.getLogger(getClass());

    private static Configuration configuration;
    private static OpenSearchClient client;
    private static ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @BeforeAll
    public static void setup() throws IOException {
        configuration = new Configuration.Builder().with(new StandardConfigurationBuilder()::addTestDefaults).build();

        URI host = configuration.getURI("opensearch.url").orElseThrow();
        client = new OpenSearchClient(new ClientConfig()
                .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger("client.opensearch")).build())
                .connectorProvider(new JettyConnectorProvider()),
                host);
    }

    @Test
    public void createComponentTemplateTest() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        InputStream templateSource = ClassLoader.getSystemResourceAsStream("/opensearch/templates/components/common.yaml");
        CreateComponentTemplateRequest createComponentTemplateRequest = new CreateComponentTemplateRequest.Builder((ObjectNode) objectMapper.readTree(templateSource)).build();

        IndexClient indices = client.indices();
        AcknowledgedResponse response = indices.createComponentTemplate("common", createComponentTemplateRequest).toCompletableFuture().get(10, TimeUnit.SECONDS);
        if (!response.isAcknowledged()) {
            logger.error("Could not load component template:\n" + response.json().toPrettyString());
        }

        Map<String, IndexTemplate> templates = indices.getComponentTemplates().toCompletableFuture().get(10, TimeUnit.SECONDS).getComponentTemplates();
        assertThat(templates.get("common"), notNullValue());
    }

    @Test
    public void createIndexTemplateTest() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        InputStream templateSource = ClassLoader.getSystemResourceAsStream("/opensearch/templates/logs.yaml");
        CreateIndexTemplateRequest createIndexTemplateRequest = new CreateIndexTemplateRequest.Builder((ObjectNode) objectMapper.readTree(templateSource)).build();

        IndexClient indices = client.indices();
        AcknowledgedResponse response = indices.createIndexTemplate("logs", createIndexTemplateRequest).toCompletableFuture().get(10, TimeUnit.SECONDS);
        if (!response.isAcknowledged()) {
            logger.error("Could not load index template:\n" + response.json().toPrettyString());
        }

        Map<String, IndexTemplate> templates = indices.getIndexTemplates().toCompletableFuture().get(10, TimeUnit.SECONDS).getIndexTemplates();
        assertThat(templates.get("logs"), notNullValue());
    }
}
