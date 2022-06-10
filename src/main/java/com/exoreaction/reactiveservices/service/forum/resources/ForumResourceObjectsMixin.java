package com.exoreaction.reactiveservices.service.forum.resources;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiCommandMixin;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiModelMixin;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResourceMixin;
import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.Included;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.contexts.PostContext;
import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.model.Posts;
import com.exoreaction.reactiveservices.service.forum.resources.api.PostResource;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
import jakarta.ws.rs.core.UriBuilder;

import java.util.function.Function;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.self;


public interface ForumResourceObjectsMixin
        extends JsonApiModelMixin, JsonApiResourceMixin, JsonApiCommandMixin {
    default GraphQuery postsQuery() {
        return new Posts(database())
                .posts()
                .result(ForumModel.Label.Post);
    }

    default Function<PostModel, ResourceObject> postResource(Included.Builder included) {

        UriBuilder postUriBuilder = getUriBuilderForPathFrom(PostResource.class);
        ForumApplication forumApplication = service(ForumApplication.class);

        return model ->
        {
            String id = model.getId();
            return new ResourceObject.Builder(ForumModel.Label.Post, id)
                    .attributes(new Attributes(model.json()))
                    .links(new Links.Builder()
                            .link(self, postUriBuilder.build(id))
                            .with(commands(postUriBuilder.clone().resolveTemplate("post", model.getId()), new PostContext(forumApplication, model))
                            ))
                    .build();
        };
    }
}
