package com.exoreaction.reactiveservices.service.forum.resources;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiModelMixin;
import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel.Post;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.model.Posts;
import com.exoreaction.reactiveservices.service.forum.resources.api.PostResource;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import com.exoreaction.reactiveservices.service.neo4j.client.NodeModel;
import com.exoreaction.reactiveservices.service.neo4j.client.RowModel;

import java.util.function.Function;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.self;


public interface ForumResourceObjectsMixin
        extends JsonApiModelMixin {
    default GraphQuery postsQuery() {
        return new Posts(database())
                .posts()
                .result(ForumModel.Label.Post);
    }

    default Function<PostModel, ResourceObject> postResource() {
        return post ->
        {
            String id = post.getId();
            return new ResourceObject.Builder(ForumModel.Label.Post, id)
                    .attributes(new Attributes(post.json()))
                    .links(new Links.Builder()
                            .link(self, getUriBuilderForPathFrom(PostResource.class).build(id)))
                    .build();
        };
    }
}
