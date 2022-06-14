package com.exoreaction.reactiveservices.service.forum.contexts;

import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.cqrs.context.DomainContext;
import com.exoreaction.reactiveservices.cqrs.metadata.DomainEventMetadata;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.aggregates.PostAggregate;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class PostContext
        implements DomainContext {
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
