package com.exoreaction.reactiveservices.service.registry.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "type")
public interface RegistryChange {
    //    @JsonValue
    ObjectNode json();
}
