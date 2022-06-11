package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.cqrs.DomainEventMetadata;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.model.*;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.contexts.PostsContext;
import com.exoreaction.reactiveservices.service.forum.resources.ForumApiMixin;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;

@Path("api/forum/posts")
public class PostsResource
        extends JsonApiResource
        implements ForumApiMixin {

    private final PostsContext context;

    @Inject
    public PostsResource(ForumApplication forumApplication) {
        context = forumApplication.posts();
    }

    @GET
    public CompletionStage<ResourceDocument> get(@QueryParam("rel") String rel) {
        if (rel != null) {
            return commandResourceDocument(rel, null, context);
        } else {
            Links.Builder links = new Links.Builder();
            Included.Builder included = new Included.Builder();
            return posts(included)
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
        return execute(resourceObject, context, metadata());
    }

    @Override
    public CompletionStage<Response> ok(Metadata metadata) {
        String aggregateId = new DomainEventMetadata(metadata).getAggregateId();
        URI location = getUriBuilderFor(PostResource.class).build(aggregateId);
        return post(aggregateId, new Included.Builder())
                .thenApply(post -> Response.created(location).links(schemaHeader()).entity(post).build());
    }
}
