package com.exoreaction.xorcery.service.eventstore.resources.api;

import com.eventstore.dbclient.StreamNotFoundException;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.service.eventstore.EventStoreService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import javax.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("api/eventstore/streams/{id}")
public class StreamResource {

    private EventStoreService eventStoreService;

    @Inject
    public StreamResource(EventStoreService eventStoreService) {
        this.eventStoreService = eventStoreService;
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
