package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.registry.resources.api.RegistryServersResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.JSON_API_TEXT_HTML;

@Path("api/forum")
public class ForumResource
        extends JsonApiResource {
    private final ForumApplication forum;

    @Inject
    public ForumResource(ForumApplication forum) {
        this.forum = forum;
    }

    @GET
    @Produces(JSON_API_TEXT_HTML)
    public ResourceDocument get() {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link("posts", getUriBuilderForPathFrom(PostsResource.class))
                        .link("post", getUriBuilderForPathFrom(PostResource.class).toTemplate())
                        .build())
                .build();
    }
}
