package com.exoreaction.reactiveservices.cqrs.model;

public interface EntityModel
        extends Model
{
    default String getId() {
        return getString(StandardModel.Entity.id);
    }
}
