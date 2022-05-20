package com.exoreaction.reactiveservices.service.conductor.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_OBJECT,property = "type")
public interface ConductorChange {
//    @JsonValue
    jakarta.json.JsonValue json();
}
