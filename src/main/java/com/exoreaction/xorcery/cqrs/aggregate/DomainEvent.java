package com.exoreaction.xorcery.cqrs.aggregate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
public interface DomainEvent {
}
