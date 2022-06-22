package com.exoreaction.xorcery.service.forum.contexts;

import com.exoreaction.xorcery.cqrs.UUIDs;
import com.exoreaction.xorcery.cqrs.aggregate.Command;
import com.exoreaction.xorcery.cqrs.context.DomainContext;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.forum.ForumApplication;
import com.exoreaction.xorcery.service.forum.model.PostModel;
import com.exoreaction.xorcery.service.forum.resources.aggregates.PostAggregate;

import java.util.List;
import java.util.concurrent.CompletionStage;

public record PostCommentsContext(ForumApplication forumApplication, PostModel postModel)
    implements DomainContext
{
    @Override
    public List<Command> commands() {
        return List.of(new PostAggregate.AddComment(UUIDs.newId(), ""));
    }

    @Override
    public CompletionStage<Metadata> handle(Metadata metadata, Command command) {
        return forumApplication.handle(new PostAggregate(), metadata, command);
    }
}
