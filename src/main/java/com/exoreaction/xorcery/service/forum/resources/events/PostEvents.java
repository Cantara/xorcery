package com.exoreaction.xorcery.service.forum.resources.events;

import com.exoreaction.xorcery.cqrs.aggregate.DomainEvent;

public interface PostEvents {
    record CreatedPost() implements DomainEvent {
    }

    record UpdatedPost(String title, String body) implements DomainEvent {
    }

    record AddedComment(String id) implements DomainEvent {
    }

    record UpdatedComment(String id, String body) implements DomainEvent {
    }
}
