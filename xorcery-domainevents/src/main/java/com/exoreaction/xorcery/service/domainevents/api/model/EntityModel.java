package com.exoreaction.xorcery.service.domainevents.api.model;

public interface EntityModel
        extends Model {
    default String getId() {
        return getString(CommonModel.Entity.id).orElse(null);
    }

    default String getAggregateId() {
        return getString(CommonModel.Aggregate.id).orElse(null);
    }
}
