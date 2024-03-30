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

import com.exoreaction.xorcery.metadata.Metadata;

import java.util.ArrayList;
import java.util.List;

public class MetadataEvents {
    private Metadata metadata;
    private List<DomainEvent> events;

    public MetadataEvents() {
    }

    public MetadataEvents(Metadata metadata, List<DomainEvent> events) {
        this.metadata = metadata;
        this.events = events;
    }

    public void set(Metadata metadata, List<DomainEvent> events) {
        this.metadata = metadata;
        this.events = events;
    }

    public void set(MetadataEvents other) {
        this.metadata = other.metadata;
        this.events = other.events;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public List<DomainEvent> getEvents() {
        return events;
    }

    // Strip away attributes and relationships for security reasons
    public MetadataEvents cloneWithoutState()
    {
        List<DomainEvent> cleanedDomainEvents = new ArrayList<>(events.size());
        for (DomainEvent domainEvent : events) {
            if (domainEvent instanceof JsonDomainEvent jde)
            {
                JsonDomainEvent jsonDomainEvent = new JsonDomainEvent(jde.json().deepCopy());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.attributes.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.addedattributes.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.removedattributes.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.updatedrelationships.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.addedrelationships.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.removedrelationships.name());
                cleanedDomainEvents.add(jsonDomainEvent);
            } else
            {
                cleanedDomainEvents.add(domainEvent);
            }
        }
        return new MetadataEvents(metadata, cleanedDomainEvents);
    }

    @Override
    public String toString() {
        return metadata.toString()+events.toString();
    }
}
