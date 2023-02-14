package com.exoreaction.xorcery.service.eventstore.resources.api;

import com.eventstore.dbclient.StreamNotFoundException;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.service.eventstore.EventStoreService;
import com.exoreaction.xorcery.service.eventstore.streams.EventStoreStreamsService;
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
