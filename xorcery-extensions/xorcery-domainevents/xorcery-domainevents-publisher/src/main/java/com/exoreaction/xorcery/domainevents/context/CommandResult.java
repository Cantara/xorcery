package com.exoreaction.xorcery.domainevents.context;

import com.exoreaction.xorcery.domainevents.api.DomainEvent;
import com.exoreaction.xorcery.domainevents.entity.Command;
import com.exoreaction.xorcery.metadata.Metadata;

import java.util.List;

public record CommandResult<T extends Command>(T command, List<DomainEvent> events, Metadata metadata) {
}
