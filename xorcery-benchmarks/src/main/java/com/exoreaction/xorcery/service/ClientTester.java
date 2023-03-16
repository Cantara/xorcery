package com.exoreaction.xorcery.service;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyWriter;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

@Service(name="clienttester")
@RunLevel(6)
public class ClientTester {

    private final JsonApiClient jsonApiClient;
    private final Client client;

    @Inject
    public ClientTester(KeyStores keyStores,
                        ClientBuilder clientBuilder,
                        Configuration configuration) {
        client = clientBuilder
                .register(JsonElementMessageBodyReader.class)
                .register(JsonElementMessageBodyWriter.class)
                .build();
        this.jsonApiClient = new JsonApiClient(client);
    }

    public CompletionStage<ResourceDocument> getResourceDocument(URI resource)
    {
        return jsonApiClient.get(new Link("self", resource.toASCIIString()));
    }

    public Future<Response> get(URI resource)
    {
        return client.target(resource).request().async().get();
    }
}
