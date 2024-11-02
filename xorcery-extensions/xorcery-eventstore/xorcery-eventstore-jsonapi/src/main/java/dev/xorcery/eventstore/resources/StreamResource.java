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
package dev.xorcery.eventstore.resources;

import com.eventstore.dbclient.StreamNotFoundException;
import dev.xorcery.jsonapi.Attributes;
import dev.xorcery.jsonapi.ResourceDocument;
import dev.xorcery.jsonapi.ResourceObject;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("api/eventstore/streams/{id}")
public class StreamResource {

    private final EventStoreStreams eventStoreService;

    @Inject
    public StreamResource(Provider<EventStoreStreams> eventStoreService) {
        this.eventStoreService = Optional.ofNullable(eventStoreService.get()).orElseThrow(NotFoundException::new);
    }

    @GET
    public CompletionStage<ResourceDocument> get(@PathParam("id") String id) {
        return eventStoreService.getStream(id)
                .thenApply(streamModel ->
                        new ResourceDocument.Builder()
                                .data(new ResourceObject.Builder("Stream", streamModel.id())
                                        .attributes(new Attributes.Builder()
                                                .attribute("revision", streamModel.revision())))
                                .build()).exceptionallyCompose(throwable ->
                {
                    if (throwable.getCause() instanceof StreamNotFoundException) {
                        return CompletableFuture.failedStage(new NotFoundException());
                    } else {
                        return CompletableFuture.failedStage(new ServerErrorException(INTERNAL_SERVER_ERROR, throwable.getCause()));
                    }
                });
    }
}
