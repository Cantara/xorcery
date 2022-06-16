package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.cqrs.metadata.DomainEventMetadata;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.jsonapi.model.Included;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.resources.JsonApiResource;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.contexts.PostCommentsContext;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.ForumApiMixin;
import com.exoreaction.reactiveservices.service.forum.resources.aggregates.PostAggregate;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.reactiveservices.cqrs.metadata.DomainEventMetadata.Builder.aggregateId;
import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;

@Path("api/forum/posts/{post}/comments")
public class PostCommentsResource
        extends JsonApiResource
        implements ForumApiMixin {

    private PostCommentsContext context;
    private PostModel post;

    @Inject
    public void bind(ForumApplication forumApplication) {
        GraphQuery graphQuery = postByIdQuery(getFirstPathParameter("post"));
        post = graphQuery
                .first(toModel(PostModel::new, graphQuery.getResults()))
                .toCompletableFuture()
                .join();
        context = forumApplication.postComments(post);
    }

    @GET
    public CompletionStage<ResourceDocument> get(@QueryParam("rel") String rel) {
        if (rel != null) {
            return commandResourceDocument(rel, null, context);
        } else {
            Links.Builder links = new Links.Builder();
            Included.Builder included = new Included.Builder();
            return postComments(post.getId(), included, pagination(links))
                    .thenApply(ros -> new ResourceDocument.Builder()
                            .data(ros)
                            .included(included.build())
                            .links(links.with(commands(getRequestUriBuilder(), context), schemaLink()))
                            .build());
        }
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded", APPLICATION_JSON_API})
    public CompletionStage<Response> post(ResourceObject resourceObject) {
        return execute(resourceObject, context, aggregateId(getFirstPathParameter("post"), metadata()));
    }

    @Override
    public CompletionStage<Response> ok(Metadata metadata, Command command) {

        if (command instanceof PostAggregate.AddComment addComment) {
            return comment(addComment.id(), new Included.Builder())
                    .thenApply(resource -> Response.created(resource.getLinks().getSelf().orElseThrow().getHrefAsUri())
                            .links(schemaHeader()).entity(resource).build());
        } else
            throw new NotFoundException();
    }
}
