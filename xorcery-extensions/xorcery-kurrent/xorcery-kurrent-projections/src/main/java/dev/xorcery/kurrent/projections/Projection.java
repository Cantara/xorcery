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
package dev.xorcery.kurrent.projections;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.json.JsonElement;

import java.util.Optional;

public record Projection(ObjectNode json)
        implements JsonElement {
    String getName() {
        return getString("name").orElseThrow();
    }

    boolean isEnabled() {
        return getBoolean("enabled").orElse(true);
    }

    Optional<String> getQuery() {
        return getString("query");
    }

    long getVersion() {
        return getLong("version").orElse(1L);
    }

    public boolean isEmitEnabled() {
        return getBoolean("emitenabled").orElse(false);
    }

    public boolean isTrackingEnabled() {
        return getBoolean("trackingenabled").orElse(false);
    }
}
