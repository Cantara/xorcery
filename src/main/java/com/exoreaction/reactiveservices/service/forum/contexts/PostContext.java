package com.exoreaction.reactiveservices.service.forum.contexts;

import com.exoreaction.reactiveservices.service.forum.model.PostModel;

public class PostContext {
    private PostModel postModel;

    public PostContext(PostModel postModel) {
        this.postModel = postModel;
    }
}
