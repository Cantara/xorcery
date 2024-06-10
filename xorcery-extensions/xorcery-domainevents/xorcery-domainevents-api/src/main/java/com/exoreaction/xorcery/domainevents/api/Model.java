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
        name,
        value,
        index,

        updatedrelationships,
        addedrelationships,
        removedrelationships,
        relationship,
        metadata
    }

    enum JsonSystemEventModel
    {
        event,
        attributes,
        metadata
    }

    enum ValueModel
    {
        attributes,
        relationships
    }
}
