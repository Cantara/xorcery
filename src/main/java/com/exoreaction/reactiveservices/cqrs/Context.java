package com.exoreaction.reactiveservices.cqrs;

import com.exoreaction.reactiveservices.disruptor.Metadata;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface Context {

    default List<Command> commands()
    {
        return Collections.emptyList();
    }

    CompletionStage<Metadata> handle(Metadata metadata, Command command);
}
