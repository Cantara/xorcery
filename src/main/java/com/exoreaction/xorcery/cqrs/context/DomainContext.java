package com.exoreaction.xorcery.cqrs.context;

import com.exoreaction.xorcery.cqrs.aggregate.Command;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import jakarta.ws.rs.NotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface DomainContext {

    class Empty
        implements DomainContext
    {
    }

    default List<Command> commands()
    {
        return Collections.emptyList();
    }

    default CompletionStage<Metadata> handle(Metadata metadata, Command command)
    {
        return CompletableFuture.failedStage(new NotFoundException());
    }
}
