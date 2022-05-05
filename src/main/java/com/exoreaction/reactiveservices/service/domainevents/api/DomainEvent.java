package com.exoreaction.reactiveservices.service.domainevents.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY,property = "eventtype")
public interface DomainEvent {
}
