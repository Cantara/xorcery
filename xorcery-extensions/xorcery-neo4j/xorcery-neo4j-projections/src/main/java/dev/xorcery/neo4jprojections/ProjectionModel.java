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
package dev.xorcery.neo4jprojections;

import java.util.Map;
import java.util.Optional;

public record ProjectionModel(Map<String, Object> map)
        implements dev.xorcery.collections.Element {

    @Override
    public <T> Optional<T> get(String name) {
        return Optional.ofNullable((T)map.get(name));
    }

    public String getProjectionId() {
        return getString(Projection.id).orElseThrow();
    }

    public long getCreatedOn() {
        return getLong(Projection.createdOn).orElseThrow();
    }

    public Optional<Long> getProjectionPosition() {
        return getLong(Projection.projectionPosition);
    }

    public Optional<String> getResourceUrl() {
        return getString(Projection.resourceUrl);
    }
}
