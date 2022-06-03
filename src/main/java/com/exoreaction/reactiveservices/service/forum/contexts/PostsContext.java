package com.exoreaction.reactiveservices.service.forum.contexts;

import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.Context;
import com.exoreaction.reactiveservices.cqrs.DomainEventMetadata;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.resources.aggregates.PostAggregate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class PostsContext
        implements Context {
    private final ForumApplication forumApplication;

    public PostsContext(ForumApplication forumApplication) {
        this.forumApplication = forumApplication;
    }

    @Override
    public List<Command> commands() {
        return List.of(new PostAggregate.CreatePost("", ""));
    }

    @Override
    public CompletionStage<Metadata> handle(Metadata metadata, Command command) {

        metadata = new DomainEventMetadata.Builder(metadata)
                .aggregateId(UUID.randomUUID().toString().replace("-", ""))
                .build().metadata();
        return forumApplication.handle(new PostAggregate(), metadata, command);
    }
}
