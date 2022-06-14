package com.exoreaction.reactiveservices.cqrs.aggregate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
public interface DomainEvent {
}
