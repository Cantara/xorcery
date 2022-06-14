package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.model.Model;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CommentModel(ObjectNode json)
        implements Model {
}
