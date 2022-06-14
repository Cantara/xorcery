package com.exoreaction.reactiveservices.service.forum.resources;

import com.exoreaction.reactiveservices.cqrs.model.StandardModel;
import com.exoreaction.reactiveservices.jsonapi.model.*;
import com.exoreaction.reactiveservices.jsonapi.resources.JsonApiResourceMixin;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.model.Posts;
import com.exoreaction.reactiveservices.service.forum.resources.api.ApiTypes;
import com.exoreaction.reactiveservices.service.forum.resources.api.ForumResource;
import com.exoreaction.reactiveservices.service.forum.resources.api.PostResource;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.describedby;
import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.self;

public interface ForumApiMixin
        extends JsonApiResourceMixin {
    default CompletionStage<ResourceObjects> posts(Included.Builder included, Links.Builder links) {
        GraphQuery graphQuery = postsQuery()
                .limit(5) // Max 5 results
                .sort(StandardModel.Entity.created_on, GraphQuery.Order.ASCENDING) // Default sorting
                .with(pagination(links), sort(ForumModel.Post.class)); // Query param pagination and sorting

        return graphQuery.stream(toModel(PostModel::new, graphQuery.getResults()).andThen(postResource(included)))
                .thenApply(ResourceObjects::toResourceObjects);
    }

    default CompletionStage<ResourceObject> post(String id, Included.Builder included) {
        GraphQuery graphQuery = postsQuery()
                .parameter(StandardModel.Entity.id, id);
        return graphQuery.first(toModel(PostModel::new, graphQuery.getResults()).andThen(postResource(included)));
    }

    @Override
    default Consumer<Links.Builder> schemaLink() {
        return links ->
        {
            links.link(describedby, getUriBuilderFor(ForumResource.class).path(".schema").build());
        };
    }

    default Link schemaHeader() {
        return Link.fromUriBuilder(getUriBuilderFor(ForumResource.class).path(".schema")).rel("describedby").build();
    }

    default GraphQuery postsQuery() {
        return new Posts(database())
                .posts()
                .results(StandardModel.Entity.values())
                .results(ForumModel.Post.values()) // Specify potential fields to return
                .with(selectedFields(ApiTypes.post, StandardModel.Label.Entity)) // Filter out fields based on query
                .results(StandardModel.Entity.id); // Id field is mandatory though
    }

    default GraphQuery postByIdQuery(String id) {
        return postsQuery().parameter(StandardModel.Entity.id, id);
    }

    default Function<PostModel, ResourceObject> postResource(Included.Builder included) {

        UriBuilder postUriBuilder = getUriBuilderFor(PostResource.class);
        ForumApplication forumApplication = forum();

        return model ->
        {
            String id = model.getId();
            return new ResourceObject.Builder(ApiTypes.post, id)
                    .attributes(new Attributes(model.json()))
                    .links(new Links.Builder()
                            .link(self, postUriBuilder.build(id))
                            .with(commands(postUriBuilder.clone().resolveTemplate("post", model.getId()), forumApplication.post(model)))
                    )
                    .build();
        };
    }

    private ForumApplication forum() {
        return service(ForumApplication.class);
    }

    @Override
    default ObjectMapper objectMapper() {
        return service(ObjectMapper.class);
    }
}
