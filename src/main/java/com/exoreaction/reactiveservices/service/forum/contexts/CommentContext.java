package com.exoreaction.reactiveservices.service.forum.contexts;

import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.cqrs.context.DomainContext;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.service.forum.ForumApplication;
import com.exoreaction.reactiveservices.service.forum.model.CommentModel;

import java.util.concurrent.CompletionStage;

public record CommentContext(ForumApplication forumApplication, CommentModel model)
    implements DomainContext
{
    @Override
    public CompletionStage<Metadata> handle(Metadata metadata, Command command) {
        return null;
    }
}
