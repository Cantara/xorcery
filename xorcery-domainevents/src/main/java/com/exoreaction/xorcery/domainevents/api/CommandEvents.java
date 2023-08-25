package com.exoreaction.xorcery.domainevents.api;

import com.exoreaction.xorcery.metadata.Metadata;

public record CommandEvents(Metadata metadata, DomainEvents domainEvents) {
}
