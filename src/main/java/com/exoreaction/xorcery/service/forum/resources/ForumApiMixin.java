package com.exoreaction.xorcery.service.forum.resources;

import com.exoreaction.xorcery.cqrs.model.CommonModel;
import com.exoreaction.xorcery.jsonapi.model.*;
import com.exoreaction.xorcery.jsonapi.resources.JsonApiResourceMixin;
import com.exoreaction.xorcery.jsonapi.resources.Pagination;
import com.exoreaction.xorcery.jsonapi.resources.ResourceObjectMapperMixin;
import com.exoreaction.xorcery.service.forum.ForumApplication;
import com.exoreaction.xorcery.service.forum.model.*;
import com.exoreaction.xorcery.service.forum.resources.api.*;
import com.exoreaction.xorcery.service.neo4j.client.GraphQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.exoreaction.xorcery.jsonapi.model.JsonApiRels.describedby;
import static com.exoreaction.xorcery.jsonapi.model.JsonApiRels.self;

public interface ForumApiMixin
        extends JsonApiResourceMixin, ResourceObjectMapperMixin {

    default CompletionStage<ResourceObjects> posts(Included.Builder included, Links.Builder links) {
        GraphQuery graphQuery = postsQuery()
                .sort(CommonModel.Entity.created_on, GraphQuery.Order.ASCENDING) // Default sorting
                .with(pagination(links), sort(ForumModel.Post.class)); // Query param pagination and sorting

        return graphQuery.stream(toModel(PostModel::new, graphQuery.getResults()).andThen(postResource(included, "")))
                .thenApply(ResourceObjects::toResourceObjects);
    }

    default CompletionStage<ResourceObject> post(String id, Included.Builder included) {
        GraphQuery graphQuery = postsQuery()
                .parameter(CommonModel.Entity.id, id);
        return graphQuery.first(toModel(PostModel::new, graphQuery.getResults()).andThen(postResource(included, "")));
    }

    default CompletionStage<ResourceObjects> postComments(String postId, Included.Builder included, Pagination pagination) {
        GraphQuery graphQuery = postCommentsQuery(postId)
                .limit(3) // Max 5 results
                .sort(CommonModel.Entity.created_on, GraphQuery.Order.ASCENDING) // Default sorting
                .with(pagination, sort(ForumModel.Comment.class)); // Query param pagination and sorting

        return graphQuery.stream(toModel(CommentModel::new, graphQuery.getResults()).andThen(commentResource(included)))
                .thenApply(ResourceObjects::toResourceObjects);
    }

    default CompletionStage<ResourceObject> comment(String id, Included.Builder included) {
        GraphQuery graphQuery = commentByIdQuery(id);
        return graphQuery.first(toModel(CommentModel::new, graphQuery.getResults()).andThen(commentResource(included)));
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
                .limit(5) // Max 5 results
                .results(CommonModel.Entity.values())
                .results(ForumModel.Post.values()) // Specify potential fields to return
                .with(selectedFields(ApiTypes.post, CommonModel.Label.Entity)) // Filter out fields based on query
                .results(CommonModel.Entity.id); // Id field is mandatory though
    }

    default GraphQuery postByIdQuery(String id) {
        return postsQuery().parameter(CommonModel.Entity.id, id);
    }

    default GraphQuery postCommentsQuery(String postId) {
        return new Comments(database())
                .commentsByPost(postId)
                .limit(5) // Max 5 results
                .results(CommonModel.Entity.values())
                .results(ForumModel.Comment.values()) // Specify potential fields to return
                .with(selectedFields(ApiTypes.comment, CommonModel.Label.Entity)) // Filter out fields based on query
                .results(CommonModel.Entity.id); // Id field is mandatory though
    }

    default GraphQuery commentByIdQuery(String id) {
        return new Comments(database())
                .comments()
                .parameter(CommonModel.Entity.id, id)
                .results(CommonModel.Entity.values())
                .results(ForumModel.Comment.values()) // Specify potential fields to return
                .with(selectedFields(ApiTypes.comment, CommonModel.Label.Entity)) // Filter out fields based on query
                .results(CommonModel.Entity.id);
    }

    default Function<PostModel, ResourceObject> postResource(Included.Builder included, String includePrefix) {
        UriBuilder uriBuilder = getUriBuilderFor(PostResource.class);
        ForumApplication forumApplication = forum();
        Predicate<String> fieldSelection = getFieldSelection(ApiTypes.post).or(getFieldSelection(CommonModel.Label.Entity));

        return this.<PostModel>resourceObjectMapper(model -> new ResourceObject.Builder(ApiTypes.post, model.getId()), included)
                .with(selfLink(model -> uriBuilder.build(model.getId())),
                        modelAttributes(fieldSelection),
                        commandLinks(model -> uriBuilder.clone().resolveTemplate("post", model.getId()), forumApplication::post),
                        modelRelationship(ApiRelationships.Post.comments, fieldSelection, includePrefix,
                                model -> relationshipRelatedLink(uriBuilder.clone().resolveTemplate("post", model.getId()), ApiRelationships.Post.comments),
                                (model, mapper, relationshipIncludePrefix) ->
                                        postComments(model.getId(), included, pagination(mapper.links(),
                                                () -> getUriBuilderFor(PostCommentsResource.class)
                                                        .replaceQuery(getUriInfo().getRequestUri().getQuery())
                                                        .resolveTemplate("post", model.getId()), includePrefix + ApiRelationships.Post.comments))
                                                .toCompletableFuture().join()))
                .build();
    }

    default Function<CommentModel, ResourceObject> commentResource(Included.Builder included) {

        UriBuilder uriBuilder = getUriBuilderFor(CommentResource.class);
        ForumApplication forumApplication = forum();

        return model ->
        {
            String id = model.getId();
            return new ResourceObject.Builder(ApiTypes.comment, id)
                    .attributes(new Attributes(model.json()))
                    .links(new Links.Builder()
                            .link(self, uriBuilder.build(id))
                            .with(commands(uriBuilder.clone().resolveTemplate("comment", model.getId()), forumApplication.comment(model)))
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
