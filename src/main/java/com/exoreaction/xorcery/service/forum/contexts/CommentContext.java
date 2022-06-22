package com.exoreaction.xorcery.service.forum.contexts;

import com.exoreaction.xorcery.cqrs.aggregate.Command;
import com.exoreaction.xorcery.cqrs.context.DomainContext;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.forum.ForumApplication;
import com.exoreaction.xorcery.service.forum.model.CommentModel;

import java.util.concurrent.CompletionStage;

public record CommentContext(ForumApplication forumApplication, CommentModel model)
    implements DomainContext
{
    @Override
    public CompletionStage<Metadata> handle(Metadata metadata, Command command) {
        return null;
    }
}
