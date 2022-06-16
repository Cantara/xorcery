package com.exoreaction.reactiveservices.cqrs.model;

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
        created_on,
        last_updated_on,
    }
}
