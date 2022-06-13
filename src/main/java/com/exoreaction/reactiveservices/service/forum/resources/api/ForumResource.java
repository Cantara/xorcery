package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonschema.model.JsonSchema;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.describedby;

@Path("api/forum")
public class ForumResource
        extends JsonApiResource {
    private final ForumApplication forum;

    @Inject
    public ForumResource(ForumApplication forum) {
        this.forum = forum;
    }

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {
        return forum.getSchema(getAbsolutePath()).schema().schema();
    }

    @GET
    public ResourceDocument get() {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(describedby, getAbsolutePathBuilder().path(".schema"))
                        .link("posts", getUriBuilderFor(PostsResource.class)
                                .queryParam("fields[post]", "{post_fields}")
                                .queryParam("sort", "{sort}")
                                .queryParam("page[skip]", "{skip}")
                                .queryParam("page[limit]", "{limit}")
                                .toTemplate())
                        .link("post", getUriBuilderFor(PostResource.class).toTemplate())
                        .build())
                .build();
    }
}
