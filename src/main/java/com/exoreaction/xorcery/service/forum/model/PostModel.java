package com.exoreaction.xorcery.service.forum.model;

import com.exoreaction.xorcery.cqrs.model.EntityModel;
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
