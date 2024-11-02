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
package dev.xorcery.opensearch.client.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.opensearch.client.OpenSearchClient;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
        configuration = new ConfigurationBuilder().addTestDefaults().build();

        URI host = configuration.getURI("opensearch.url").orElseThrow();
        client = new OpenSearchClient(ClientBuilder.newBuilder()
                .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger("client.opensearch")).build()),
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
