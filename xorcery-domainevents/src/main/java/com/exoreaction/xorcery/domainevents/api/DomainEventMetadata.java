package com.exoreaction.xorcery.domainevents.api;

/**
 * These are all the metadata fields that can be sent with domain events
 */
public enum DomainEventMetadata {
    // Source related
    agent, // client identifier, e.g. "eventsdk/1.0"
    source, // source identifier, e.g. "myapp/2.2"
    host, // Host identifier, IP or hostname

    // Command related
    timestamp, // Timestamp of when events were issued, in epoch milliseconds
    correlationId, // id of request (optional)
    commandName, //  Name of action/screen/form used to create these events, e.g. "RegisterUser"
    validFrom, // ISO-8601 string of when events become valid (optional)
    reason, // User specified reason for change (optional)

    // Scope related
    domain, // Group of events this belongs to
    tenantId, // Id of tenant (optional)
    aggregateType, // Type of aggregate of these events
    aggregateId, // Id for aggregate of these events

    // Identification related
    userId, // Id of user, identifies the user actually creating the events
    roleId, // Role id of user (optional)
    actingAsId, // Role id of user on behalf-of whom the events are created (optional)
    jwt // Client JWT token
}
