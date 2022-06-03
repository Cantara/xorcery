package com.exoreaction.reactiveservices.service.forum.contexts;

import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.Context;
import com.exoreaction.reactiveservices.cqrs.DomainEventMetadata;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.aggregates.PostAggregate;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class PostContext
        implements Context {
    private ForumApplication forumApplication;
    private PostModel postModel;

    public PostContext(ForumApplication forumApplication, PostModel postModel) {
        this.forumApplication = forumApplication;
        this.postModel = postModel;
    }

    @Override
    public List<Command> commands() {
        return List.of(new PostAggregate.UpdatePost(postModel.getTitle(), postModel.getBody()));
    }

    @Override
    public CompletionStage<Metadata> handle(Metadata metadata, Command command) {
        metadata = new DomainEventMetadata.Builder(metadata)
                .aggregateId(postModel.getId())
                .build().metadata();
        return forumApplication.handle(new PostAggregate(), metadata, command);
    }
}
