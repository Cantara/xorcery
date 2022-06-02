package com.exoreaction.reactiveservices.service.forum.model;

import com.exoreaction.reactiveservices.cqrs.Model;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;

public record PostModel(ResourceObject resourceObject)
    implements Model
{
}
