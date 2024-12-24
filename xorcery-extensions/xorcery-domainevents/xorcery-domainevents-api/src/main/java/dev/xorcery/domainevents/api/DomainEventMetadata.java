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
package dev.xorcery.domainevents.api;

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
    environment, // environment identifier, e.g. development, qa, staging, production
    domain, // Group of events this belongs to
    tenantId, // Id of tenant (optional)
    aggregateType, // Type of aggregate of these events
    aggregateId, // Id for aggregate of these events

    // Identification related
    userId, // Id of user, identifies the user actually creating the events
    roleId, // Role id of user (optional)
    actingAsId, // Role id of user on behalf-of whom the events are created (optional)
    jwt, // Client JWT token

    // Processing data
    streamPosition // Position of events in stream
}
