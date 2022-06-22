package com.exoreaction.xorcery.service.opensearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.*;
import org.opensearch.client.indices.*;
import org.opensearch.common.io.stream.OutputStreamStreamOutput;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

@Disabled
public class ConnectionTest {

    @Test
    public void testConnect()
            throws Exception {
        RestHighLevelClient client = getRestHighLevelClient();

        //Create a non-default index with custom settings and mappings.
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("custom-index");

        createIndexRequest.settings(Settings.builder() //Specify in the settings how many shards you want in the index.
                .put("index.number_of_shards", 4)
                .put("index.number_of_replicas", 1)
        );
        //Create a set of maps for the index's mappings.
        HashMap<String, String> typeMapping = new HashMap<String, String>();
        typeMapping.put("type", "integer");
        HashMap<String, Object> ageMapping = new HashMap<String, Object>();
        ageMapping.put("age", typeMapping);
        HashMap<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("properties", ageMapping);
        createIndexRequest.mapping(mapping);
        CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);

        //Adding data to the index.
        IndexRequest request = new IndexRequest("custom-index"); //Add a document to the custom-index we created.
        request.id("1"); //Assign an ID to the document.

        HashMap<String, String> stringMapping = new HashMap<String, String>();
        stringMapping.put("message:", "Testing Java REST client");
        request.source(stringMapping); //Place your content into the index's source.
        IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);

        //Getting back the document
        GetRequest getRequest = new GetRequest("custom-index", "1");
        GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);

        System.out.println(response.getSourceAsString());

        //Delete the document
        DeleteRequest deleteDocumentRequest = new DeleteRequest("custom-index", "1"); //Index name followed by the ID.
        DeleteResponse deleteResponse = client.delete(deleteDocumentRequest, RequestOptions.DEFAULT);

        //Delete the index
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("custom-index"); //Index name.
        AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);

        client.close();
    }

    @NotNull
    private RestHighLevelClient getRestHighLevelClient() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("admin", "admin"));

        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

    @Test
    public void testTemplates() throws IOException, URISyntaxException {
        RestHighLevelClient client = getRestHighLevelClient();
        List<IndexTemplateMetadata> templates = client.indices().getIndexTemplate(new GetIndexTemplatesRequest(), RequestOptions.DEFAULT).getIndexTemplates();

        for (IndexTemplateMetadata template : templates) {
            System.out.println(template.name());
        }

        // Upload template
        PutIndexTemplateRequest putIndexTemplateRequest = new PutIndexTemplateRequest("metrics");
        putIndexTemplateRequest.source(Files.readString(Path.of(getClass().getResource("/opensearch/common.json").toURI()), Charset.defaultCharset()), XContentType.JSON);
        client.indices().putTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT).writeTo(new OutputStreamStreamOutput(System.out));
    }
}
