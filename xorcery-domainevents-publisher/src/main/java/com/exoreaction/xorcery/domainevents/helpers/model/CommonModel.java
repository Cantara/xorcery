package com.exoreaction.xorcery.domainevents.helpers.model;

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
