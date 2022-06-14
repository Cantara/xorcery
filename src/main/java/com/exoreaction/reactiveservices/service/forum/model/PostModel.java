package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.model.EntityModel;
import com.exoreaction.reactiveservices.cqrs.model.Model;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record PostModel(ObjectNode json)
        implements EntityModel {
    public String getTitle() {
        return getString(ForumModel.Post.title);
    }

    public String getBody() {
        return getString(ForumModel.Post.body);
    }
}
