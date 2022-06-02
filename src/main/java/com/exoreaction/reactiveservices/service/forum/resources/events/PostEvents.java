package com.exoreaction.reactiveservices.service.forum.resources.events;

import com.exoreaction.reactiveservices.cqrs.DomainEvent;

public interface PostEvents {
    record CreatedPost() implements DomainEvent {
    }

    record UpdatedPost(String title, String body) implements DomainEvent {
    }
}
