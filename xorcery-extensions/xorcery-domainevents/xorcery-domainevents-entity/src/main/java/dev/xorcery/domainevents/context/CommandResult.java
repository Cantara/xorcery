package dev.xorcery.domainevents.context;

import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.metadata.Metadata;

import java.util.List;

public record CommandResult(Command command, List<DomainEvent> events, Metadata metadata) {
}
