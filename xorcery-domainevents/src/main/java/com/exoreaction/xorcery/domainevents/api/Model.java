package com.exoreaction.xorcery.domainevents.api;

/**
 * This describes all the known fields and constants used.
 */
public interface Model {

    /**
     * These are all the metadata fields that can be sent with domain events
     */
    enum Metadata
    {
        // Source related
        agent, // client identifier, e.g. "eventsdk/1.0"
        source, // source identifier, e.g. "myapp/2.2"
        host, // Host identifier, IP or hostname

        // Command related
        commandName, //  Name of action/screen/form used to create these events, e.g. "RegisterUser"
        timestamp, // Timestamp of when events were issued, in epoch milliseconds
        correlationId, // id of request (optional)
        domain, // Group of events this belongs to (optional)
        reason, // User specified reason for change (optional)

        // Identification related
        userId, // Id of user, identifies the user actually creating the events
        roleId, // Role id of user (optional)
    }

    enum JsonDomainEventModel
    {
        event,
        created,
        updated,
        deleted,
        type,
        id,
        attributes,
        updatedrelationships,
        addedrelationships,
        removedrelationships,
        relationship,
        metadata
    }
}
