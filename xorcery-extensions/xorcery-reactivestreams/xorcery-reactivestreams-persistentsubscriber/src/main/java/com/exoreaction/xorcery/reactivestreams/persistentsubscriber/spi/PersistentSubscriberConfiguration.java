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
package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Optional;

public record PersistentSubscriberConfiguration(ObjectNode json)
        implements JsonElement {

    public String getName() {
        return getString("name").orElseThrow(Configuration.missing("name"));
    }

    public URI getURI() {
        return getURI("uri").orElseThrow(Configuration.missing("uri"));
    }

    public String getStream() {
        return getString("stream").orElseThrow(Configuration.missing("stream"));
    }

    public Optional<String> getCheckpointProvider() {
        return getString("checkpointProvider");
    }

    public Optional<String> getErrorLogProvider() {
        return getString("errorLogProvider");
    }

    public Configuration getConfiguration() {
        return new Configuration(json).getConfiguration("configuration");
    }
}
