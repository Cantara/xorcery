package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.Model;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record PostModel(ObjectNode json)
        implements Model {
    public String getId() {
        return getString(ForumModel.Post.id);
    }

    public String getTitle() {
        return getString(ForumModel.Post.title);
    }

    public String getBody() {
        return getString(ForumModel.Post.body);
    }
}
