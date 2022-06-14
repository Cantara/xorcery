package com.exoreaction.reactiveservices.cqrs.context;

import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface DomainContext {

    default List<Command> commands()
    {
        return Collections.emptyList();
    }

    CompletionStage<Metadata> handle(Metadata metadata, Command command);
}
