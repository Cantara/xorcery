package com.exoreaction.reactiveservices.service.forum.resources;

import com.exoreaction.reactiveservices.jsonapi.resources.JsonApiResourceMixin;
import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.Included;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.model.Posts;
import com.exoreaction.reactiveservices.service.forum.resources.api.ApiTypes;
import com.exoreaction.reactiveservices.service.forum.resources.api.PostResource;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import jakarta.ws.rs.core.UriBuilder;

import java.util.function.Function;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.self;


public interface ForumResourceObjectsMixin
        extends JsonApiResourceMixin {

    default GraphQuery postsQuery() {
        return new Posts(database())
                .posts()
                .results(ForumModel.Post.values()) // Specify potential fields to return
                .with(selectedFields(ApiTypes.post)) // Filter out fields based on query
                .results(ForumModel.Post.id); // Id field is mandatory though
    }

    default GraphQuery postByIdQuery(String id) {
        return postsQuery().parameter(ForumModel.Post.id, id);
    }

    default Function<PostModel, ResourceObject> postResource(Included.Builder included) {

        UriBuilder postUriBuilder = getUriBuilderFor(PostResource.class);
        ForumApplication forumApplication = forum();

        return model ->
        {
            String id = model.getId();
            return new ResourceObject.Builder(ForumModel.Label.Post, id)
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
}
