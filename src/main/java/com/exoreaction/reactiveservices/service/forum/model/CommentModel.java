package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.Model;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CommentModel(ObjectNode json)
        implements Model {
}
