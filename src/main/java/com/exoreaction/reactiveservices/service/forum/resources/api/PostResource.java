package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiCommandMixin;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.contexts.PostContext;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.ForumResourceObjectsMixin;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;

@Path("api/forum/posts/{post}")
public class PostResource
        extends JsonApiResource
        implements ForumResourceObjectsMixin, JsonApiCommandMixin {

    private PostModel post;
    private PostContext context;

    @Inject
    public void bind(ForumApplication forumApplication) {
        post = postsQuery()
                .parameter(ForumModel.Post.id, getFirstPathParameter("post"))
                .stream(toModel(PostModel::new, ForumModel.Post.class))
                .toCompletableFuture()
                .join()
                .findFirst().orElseThrow(NotFoundException::new);
        context = forumApplication.post(post);
    }

    @GET
    public CompletionStage<JsonElement> get(@QueryParam("rel") String rel) {
        if (rel != null) {
            return commandResourceObject(rel, context);
        } else {
            return CompletableFuture.completedStage(
                    new ResourceDocument.Builder()
                            .data(postResource().apply(post))
                            .build());
        }
    }

    @POST
    @Consumes({"application/x-www-form-urlencoded", APPLICATION_JSON_API})
    @Produces(APPLICATION_JSON_API)
    public CompletionStage<Response> post(ResourceObject resourceObject) {
        return execute(resourceObject, context, metadata());
    }

    @Override
    public CompletionStage<Response> ok(Metadata metadata) {
        return get(null).thenApply(rd -> Response.ok(rd).build());
    }
}
