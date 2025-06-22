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
package dev.xorcery.kurrent.jsonapi.resources;

import dev.xorcery.jsonapi.ResourceDocument;
import io.kurrent.dbclient.StreamNotFoundException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static dev.xorcery.jsonapi.Attributes.newAttributes;
import static dev.xorcery.jsonapi.ResourceDocument.newResourceDocument;
import static dev.xorcery.jsonapi.ResourceObject.newResource;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("api/kurrent/streams/{id}")
public class StreamResource {

    private final KurrentStreams kurrentStreams;

    @Inject
    public StreamResource(Provider<KurrentStreams> kurrentStreams) {
        this.kurrentStreams = Optional.ofNullable(kurrentStreams.get()).orElseThrow(NotFoundException::new);
    }

    @GET
    public CompletionStage<ResourceDocument> get(@PathParam("id") String id) {
        return kurrentStreams.getStream(id)
                .thenApply(streamModel ->
                        newResourceDocument()
                                .data(newResource("Stream", streamModel.id())
                                        .attributes(newAttributes()
                                                .attribute("revision", streamModel.revision())
                                                .attribute("lastUpdatedOn", streamModel.lastUpdatedOn())
                                        ))
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
