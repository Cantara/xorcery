package com.exoreaction.reactiveservices.service.registry.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
import jakarta.json.JsonObject;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_OBJECT,property = "type")
public interface RegistryChange {
//    @JsonValue
    jakarta.json.JsonValue json();
}
