package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.jsonapi.model.Included;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.resources.JsonApiResource;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.contexts.CommentContext;
import com.exoreaction.reactiveservices.service.forum.contexts.PostContext;
import com.exoreaction.reactiveservices.service.forum.model.CommentModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.ForumApiMixin;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;

@Path("api/forum/comments/{comment}")
public class CommentResource
        extends JsonApiResource
        implements ForumApiMixin {

    private CommentModel model;
    private CommentContext context;

    @Inject
    public void bind(ForumApplication forumApplication) {
        GraphQuery graphQuery = commentByIdQuery(getFirstPathParameter("comment"));
        model = graphQuery
                .first(toModel(CommentModel::new, graphQuery.getResults()))
                .toCompletableFuture()
                .join();
        context = forumApplication.comment(model);
    }

    @GET
    public CompletionStage<ResourceDocument> get(@QueryParam("rel") String rel) {
        if (rel != null) {
            return commandResourceDocument(rel, model.getId(), context);
        } else {
            Links.Builder links = new Links.Builder();
            Included.Builder included = new Included.Builder();
            return CompletableFuture.completedStage(
                    new ResourceDocument.Builder()
                            .data(commentResource(included).apply(model))
                            .included(included)
                            .links(links.with(schemaLink()))
                            .build());
        }
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded", APPLICATION_JSON_API})
    public CompletionStage<Response> post(ResourceObject resourceObject) {
        return execute(resourceObject, context, metadata());
    }

    @PATCH
    @Consumes({"application/x-www-form-urlencoded", APPLICATION_JSON_API})
    public CompletionStage<Response> patch(ResourceObject resourceObject) {
        return execute(resourceObject, context, metadata());
    }

    @Override
    public CompletionStage<Response> ok(Metadata metadata, Command command) {
        return comment(model.getId(), new Included.Builder())
                .thenApply(resource -> Response.ok(resource).links(schemaHeader()).build());
    }
}
