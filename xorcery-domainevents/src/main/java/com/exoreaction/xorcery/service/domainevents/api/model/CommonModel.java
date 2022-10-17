package com.exoreaction.xorcery.service.domainevents.api.model;

public interface CommonModel {

    enum Label {
        Entity,
        Aggregate,
    }

    enum Entity {
        id,
        aggregate_id,
        external_id,
        created_on,
        last_updated_on,
    }
}
