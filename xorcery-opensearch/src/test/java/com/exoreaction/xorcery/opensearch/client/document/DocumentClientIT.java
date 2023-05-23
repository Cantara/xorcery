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
package com.exoreaction.xorcery.opensearch.client.document;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.opensearch.client.index.CreateComponentTemplateRequest;
import com.exoreaction.xorcery.opensearch.client.index.CreateIndexTemplateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Disabled
public class DocumentClientIT {

    String json = """
            {
                "@timestamp": 1659070622328,
                "metadata": {
                  "environment": "development",
                  "tag": "default",
                  "version": "0.1.0",
                  "name": "genericserver",
                  "host": "localhost",
                  "timestamp": 1659070622328,
                  "agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
                  "aggregateId": "5b89b3b6b28f476abc572e15e99e06ed",
                  "domain": "forum",
                  "aggregateType": "com.exoreaction.xorcery.forum.resources.aggregates.PostEntity",
                  "commandType": "com.exoreaction.xorcery.forum.resources.aggregates.PostEntity$CreatePost",
                  "streamId": "development-default-forum",
                  "revision": 0,
                  "contentType": "application/octet-stream"
                },
                "data": [
                  {
                    "@class": "com.exoreaction.xorcery.forum.resources.events.PostEvents$CreatedPost"
                  },
                  {
                    "@class": "com.exoreaction.xorcery.forum.resources.events.PostEvents$UpdatedPost",
                    "title": "Test",
                    "body": "Test"
                  }
                ]
              }
            """;

    private Logger logger = LogManager.getLogger(getClass());

    private static Configuration configuration;
    private static OpenSearchClient client;
    private static ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @BeforeAll
    public static void setup() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        configuration = new Configuration.Builder().with(new StandardConfigurationBuilder()::addTestDefaults).build();

        URI host = configuration.getURI("opensearch.url").orElseThrow();
        client = new OpenSearchClient(ClientBuilder.newBuilder()
                .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger("client.opensearch")).build()),
                host);


        List<String> testIndices = client.indices().getIndices().toCompletableFuture().get(10, TimeUnit.SECONDS)
                .keySet().stream().filter(n -> n.startsWith("test")).collect(Collectors.toList());
        for (String testIndex : testIndices) {
            client.indices().deleteIndex(testIndex).toCompletableFuture().get(10, TimeUnit.SECONDS);
        }

        // Upload test index template
        {
            ObjectNode template = (ObjectNode) objectMapper.readTree(DocumentClientIT.class.getResource("/opensearch/templates/components/common.yaml"));
            client.indices().createComponentTemplate("common", new CreateComponentTemplateRequest(template))
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
        {
            ObjectNode template = (ObjectNode) objectMapper.readTree(DocumentClientIT.class.getResource("testindextemplate.json"));
            client.indices().createIndexTemplate("test", new CreateIndexTemplateRequest(template))
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void bulkIndexCreateTest() throws IOException, ExecutionException, InterruptedException, TimeoutException {

        ObjectNode document = (ObjectNode) objectMapper.readTree(json);

        IndexBulkRequest.Builder bulkRequest = new IndexBulkRequest.Builder();

        bulkRequest.create("localhost1", document);
        bulkRequest.create("localhost2", document);
        bulkRequest.create("localhost3", document);
        bulkRequest.create("localhost4", document);
        bulkRequest.create("localhost5", document);
        bulkRequest.create("localhost6", document);
        bulkRequest.create("localhost7", document);
        bulkRequest.create("localhost8", document);

        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault());
        String indexName = "test-" + date.getYear() + "-" + date.getMonthValue();
        BulkResponse bulkResponse = client.documents().bulk(indexName, bulkRequest.build())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);

        if (bulkResponse.hasErrors()) {
            LogManager.getLogger(getClass()).error("OpenSearch update errors:\n" + bulkResponse.json().toPrettyString());
        }
    }
}
