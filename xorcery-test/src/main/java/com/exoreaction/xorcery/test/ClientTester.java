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
package com.exoreaction.xorcery.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.providers.JsonElementMessageBodyWriter;
import com.exoreaction.xorcery.jsonapi.Link;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.keystores.KeyStores;
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
    public ClientTester(ClientBuilder clientBuilder) {
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
