package com.exoreaction.xorcery.cqrs.model;

public interface CommonModel {

    enum Label {
        Entity,
        Aggregate,
    }

    enum Aggregate {
        id
    }

    enum Entity {
        id,
        external_id,
        created_on,
        last_updated_on,
    }
}
