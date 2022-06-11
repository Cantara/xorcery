package com.exoreaction.reactiveservices.service.forum.resources;

import com.exoreaction.reactiveservices.jsonapi.model.Included;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjects;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.api.ForumResource;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import jakarta.ws.rs.core.Link;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.describedby;

public interface ForumApiMixin
        extends ForumResourceObjectsMixin {
    default CompletionStage<ResourceObjects> posts(Included.Builder included) {
        GraphQuery graphQuery = postsQuery();
        return graphQuery.stream(toModel(PostModel::new, ForumModel.Post.class).andThen(postResource(included)))
                .thenApply(ResourceObjects::toResourceObjects);
    }

    default CompletionStage<ResourceObject> post(String id, Included.Builder included) {
        GraphQuery graphQuery = postsQuery()
                .parameter(ForumModel.Post.id, id);
        return graphQuery.first(toModel(PostModel::new, ForumModel.Post.class).andThen(postResource(included)));
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
}
