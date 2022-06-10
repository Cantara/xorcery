package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.reactiveservices.jsonapi.model.*;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.contexts.PostsContext;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.ForumResourceObjectsMixin;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.CompletionStage;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;
import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.JSON_API_TEXT_HTML;

@Path("api/forum/posts")
public class PostsResource
        extends JsonApiResource
        implements ForumResourceObjectsMixin {
    private final PostsContext posts;

    @Inject
    public PostsResource(ForumApplication forumApplication) {
        posts = forumApplication.posts();
    }

    @GET
    @Produces(JSON_API_TEXT_HTML)
    public CompletionStage<JsonElement> get(@QueryParam("rel") String rel) {
        if (rel != null) {
            return commandResourceObject(rel, posts);
        } else {
            Links.Builder links = new Links.Builder();

            GraphQuery graphQuery = postsQuery();

            Included.Builder included = new Included.Builder();
            return graphQuery.stream(toModel(PostModel::new, ForumModel.Post.class).andThen(postResource(included)))
                    .thenApply(ro -> new ResourceDocument.Builder()
                            .data(ResourceObjects.toResourceObjects(ro))
                            .included(included.build())
                            .links(links.with(commands(getRequestUriBuilder(), posts)))
                            .build());
        }
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded", APPLICATION_JSON_API})
    @Produces(APPLICATION_JSON_API)
    public CompletionStage<Response> post(ResourceObject resourceObject) {
        return execute(resourceObject, posts, metadata());
    }

    @Override
    public CompletionStage<Response> ok(Metadata metadata) {
        return get(null).thenApply(rd -> Response.ok(rd).build());
    }
}
