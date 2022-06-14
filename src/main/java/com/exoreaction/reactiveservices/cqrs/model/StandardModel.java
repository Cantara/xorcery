package com.exoreaction.reactiveservices.cqrs.model;

public interface StandardModel {

    enum Label {
        Entity,
    }

    enum Entity {
        id,
        created_on,
        last_updated_on,
    }
}
