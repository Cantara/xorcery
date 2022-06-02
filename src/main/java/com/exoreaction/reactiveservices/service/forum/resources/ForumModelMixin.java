package com.exoreaction.reactiveservices.service.forum.resources;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiModelMixin;
import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel.Post;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.model.Posts;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.function.Function;


public interface ForumModelMixin
    extends JsonApiModelMixin
{
    default GraphQuery postsQuery()
    {
        return new Posts(database())
                .posts()
                .result(ForumModel.Label.Post);
    }

    default Function<Result.ResultRow, PostModel> postModel()
    {
        return row ->
        {
            Node post = row.getNode(ForumModel.Label.Post.name());
            return new PostModel(new ResourceObject.Builder(ForumModel.Label.Post.name(), post.getProperty(ForumModel.Post.id.name()).toString())
                    .attributes(new Attributes.Builder()
                            .attribute(Post.title.name(), post.getProperty(Post.title.name() ))
                            .attribute(Post.body.name(), post.getProperty(Post.body.name()) )
                            .build())
                    .build());
        };
    }
}
