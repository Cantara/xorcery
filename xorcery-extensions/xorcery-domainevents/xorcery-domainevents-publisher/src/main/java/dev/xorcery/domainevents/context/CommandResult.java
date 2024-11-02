package dev.xorcery.domainevents.context;

import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.entity.Command;
import dev.xorcery.metadata.Metadata;

import java.util.List;

public record CommandResult<T extends Command>(T command, List<DomainEvent> events, Metadata metadata) {
}
