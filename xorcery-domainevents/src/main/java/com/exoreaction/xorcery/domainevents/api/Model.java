/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        aggregateId, // Id for aggregate of these events
        aggregateType, // Type of aggregate of these events

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
        addedattributes,
        removedattributes,
        updatedrelationships,
        addedrelationships,
        removedrelationships,
        relationship,
        metadata
    }

    enum ValueModel
    {
        attributes,
        relationships
    }
}
