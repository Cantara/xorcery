package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.model.EntityModel;
import com.exoreaction.reactiveservices.cqrs.model.Model;
import com.exoreaction.reactiveservices.cqrs.model.CommonModel;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CommentModel(ObjectNode json)
        implements EntityModel {

    public String getBody() {
        return getString(ForumModel.Comment.body);
    }
}
