package com.exoreaction.reactiveservices.service.forum.resources.api;

import com.exoreaction.reactiveservices.cqrs.model.CommonModel;
import com.exoreaction.reactiveservices.hyperschema.model.Link;
import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.resources.JsonSchemaMixin;
import com.exoreaction.reactiveservices.jsonapi.schema.ResourceDocumentSchema;
import com.exoreaction.reactiveservices.jsonapi.schema.ResourceObjectSchema;
import com.exoreaction.reactiveservices.jsonschema.model.JsonSchema;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.resources.ForumApiMixin;
import com.exoreaction.reactiveservices.service.forum.resources.aggregates.PostAggregate;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.describedby;

@Path("api/forum")
public class ForumResource
        extends JsonApiResource
        implements JsonSchemaMixin {

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {
        return getSchema().schema().schema();
    }

    @GET
    public ResourceDocument get() {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(describedby, getAbsolutePathBuilder().path(".schema"))
                        .link("posts", getUriBuilderFor(PostsResource.class)
                                .queryParam("fields[post]", "{post_fields}")
                                .queryParam("fields[comment]", "{comment_fields}")
                                .queryParam("fields[entity]", "{entity_fields}")
                                .queryParam("include", "{include}")
                                .queryParam("sort", "{sort}")
                                .queryParam("page[skip]", "{skip}")
                                .queryParam("page[limit]", "{limit}")
                                .toTemplate())
                        .link("post", getUriBuilderFor(PostResource.class).toTemplate())
                        .build())
                .build();
    }

    private ResourceDocumentSchema getSchema() {
        ResourceDocumentSchema.Builder builder = new ResourceDocumentSchema.Builder();
        builder.resources(postSchema(), commentSchema())
                .included(commentSchema())
                .builder()
                .links(new com.exoreaction.reactiveservices.hyperschema.model.Links.Builder()
                        .link(selfLink()).link(describedbyLink(getAbsolutePath().toASCIIString()))
                        .with(commands(PostAggregate.class),
                                l -> l.link(new Link.UriTemplateBuilder("posts")
                                        .parameter("post_fields", "Post fields", "Post fields to include")
                                        .parameter("comment_fields", "Comment fields", "Comment fields to include")
                                        .parameter("entity_fields", "Entity fields", "Entity fields to include")
                                        .parameter("include", "Included relationships", "Relations to include")
                                        .parameter("sort", "Sort", "Post sort field")
                                        .parameter("skip", "Skip", "Nr of posts to skip")
                                        .parameter("limit", "Limit", "Limit nr of posts")
                                        .build()))
                        .build())
                .builder()
                .title("Forum application");
        return builder.build();
    }

    private ResourceObjectSchema postSchema() {
        return new ResourceObjectSchema.Builder()
                .type(ApiTypes.post)
                .relationships(relationships(ApiRelationships.Post.values()))
                .attributes(attributes(CommonModel.Entity.values()))
                .attributes(attributes(ForumModel.Post.values()))
                .with(b -> b.builder().builder().title("Post"))
                .build();
    }

    private ResourceObjectSchema commentSchema() {
        return new ResourceObjectSchema.Builder()
                .type(ApiTypes.comment)
                .attributes(attributes(CommonModel.Entity.values()))
                .attributes(attributes(ForumModel.Comment.values()))
                .with(b -> b.builder().builder().title("Comment"))
                .build();
    }

}
