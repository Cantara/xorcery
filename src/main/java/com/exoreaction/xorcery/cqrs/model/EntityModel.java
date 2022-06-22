package com.exoreaction.xorcery.cqrs.model;

public interface EntityModel
        extends Model
{
    default String getId() {
        return getString(CommonModel.Entity.id);
    }
}
