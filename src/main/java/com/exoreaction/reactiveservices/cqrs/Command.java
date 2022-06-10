package com.exoreaction.reactiveservices.cqrs;

import com.exoreaction.reactiveservices.cqrs.annotations.Create;
import com.exoreaction.reactiveservices.cqrs.annotations.Delete;
import com.exoreaction.reactiveservices.cqrs.annotations.Update;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonIgnoreProperties({ "create", "update","delete" })
public interface Command {

    default String name() {
        return getClass().getSimpleName();
    }

    default boolean isCreate() {
        return getClass().getAnnotation(Create.class) != null;
    }

    default boolean isUpdate() {
        return getClass().getAnnotation(Update.class) != null;
    }

    default boolean isDelete() {
        return getClass().getAnnotation(Delete.class) != null;
    }
}
