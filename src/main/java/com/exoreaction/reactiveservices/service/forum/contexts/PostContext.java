package com.exoreaction.reactiveservices.service.forum.contexts;

import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.cqrs.context.DomainContext;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.model.PostModel;
import com.exoreaction.reactiveservices.service.forum.resources.aggregates.PostAggregate;

import java.util.List;
import java.util.concurrent.CompletionStage;

public record PostContext(ForumApplication forumApplication, PostModel postModel)
        implements DomainContext {

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
