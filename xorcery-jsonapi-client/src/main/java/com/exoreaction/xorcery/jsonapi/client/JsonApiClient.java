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
package com.exoreaction.xorcery.jsonapi.client;

import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.jsonapi.Link;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.ResourceObject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.MediaType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public record JsonApiClient(Client client) {

    private static final MediaType APPLICATION_JSON_API_TYPE = MediaType.valueOf(MediaTypes.APPLICATION_JSON_API);
    private static final MediaType APPLICATION_JSON_SCHEMA_TYPE = MediaType.valueOf(MediaTypes.APPLICATION_JSON_SCHEMA);
    private static final MediaType APPLICATION_YAML_TYPE = MediaType.valueOf(MediaTypes.APPLICATION_YAML);

    public CompletionStage<ResourceDocument> get(Link link)
    {
        CompletableFuture<ResourceDocument> future = new CompletableFuture<>();
        client.target(link.getHrefAsUri())
                .request(APPLICATION_JSON_API_TYPE)
                .buildGet()
                .submit(new ResourceDocumentCallback(future, link));
        return future;
    }


    public CompletionStage<ResourceObject> submit(Link link, ResourceObject resourceObject) {
        CompletableFuture<ResourceObject> future = new CompletableFuture<>();
        client.target(link.getHrefAsUri())
                .request(APPLICATION_JSON_API_TYPE)
                .buildPost(Entity.entity(resourceObject, APPLICATION_JSON_API_TYPE))
                .submit(new ResourceObjectCallback(future, link));
        return future;
    }

    /**
     * For batch uploads
     *
     * @param link
     * @param resourceDocument
     * @return
     */
    public CompletionStage<ResourceObject> submit(Link link, ResourceDocument resourceDocument) {
        CompletableFuture<ResourceObject> future = new CompletableFuture<>();
        client.target(link.getHrefAsUri())
                .request(APPLICATION_JSON_API_TYPE)
                .buildPost(Entity.entity(resourceDocument, APPLICATION_JSON_API_TYPE))
                .submit(new ResourceObjectCallback(future, link));
        return future;
    }

    private static class Callback<T>
    implements InvocationCallback<T> {

        private final CompletableFuture<T> future;
        private final Link link;

        public Callback(CompletableFuture<T> future, Link link) {
            this.future = future;
            this.link = link;
        }

        @Override
        public void completed(T result) {
            future.complete(result);
        }

        @Override
        public void failed(Throwable throwable) {
            if (throwable instanceof ProcessingException processingException)
            {
                future.completeExceptionally(new ProcessingException(link.getHref(), processingException.getCause()));
            } else
            {
                future.completeExceptionally(throwable);
            }
        }
    }

    private static class ResourceObjectCallback
        extends Callback<ResourceObject>
    {
        public ResourceObjectCallback(CompletableFuture<ResourceObject> future, Link link) {
            super(future, link);
        }
    }

    private static class ResourceDocumentCallback
        extends Callback<ResourceDocument>
    {
        public ResourceDocumentCallback(CompletableFuture<ResourceDocument> future, Link link) {
            super(future, link);
        }
    }

}
