package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.cqrs.Model;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiCommandMixin;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiModelMixin;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.model.Error;
import com.exoreaction.reactiveservices.jsonapi.model.Errors;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjects;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.contexts.PostsContext;
import com.exoreaction.reactiveservices.service.forum.resources.ForumModelMixin;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;

@Path("api/forum/posts")
public class PostsResource
        extends JsonApiResource
        implements ForumModelMixin, JsonApiCommandMixin {

    private final PostsContext posts;

    @Inject
    public PostsResource(ForumApplication forumApplication) {
        posts = forumApplication.posts();
    }

    @GET
    public CompletionStage<ResourceDocument> get() {
        return postsQuery().stream(postModel()).thenApply(s ->
                new ResourceDocument.Builder()
                        .data(s.map(Model::resourceObject).collect(ResourceObjects.toResourceObjects()))
                        .build()).exceptionally(throwable ->
        {
            return new ResourceDocument.Builder()
                    .errors(new Errors.Builder()
                            .error(new Error.Builder()
                                    .status(500)
                                    .title(throwable.getMessage())
                                    .detail(throwable.toString())
                                    .build())
                            .build()).build();
        });
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded", APPLICATION_JSON_API})
    @Produces(APPLICATION_JSON_API)
    public CompletionStage<Response> post(ResourceDocument resourceDocument) {
        return execute(metadata(), resourceDocument, posts);
    }

    @Override
    public CompletionStage<Response> ok(Metadata metadata) {
        return get().thenApply(rd -> Response.ok(rd).build());
    }
}
