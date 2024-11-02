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
package dev.xorcery.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.builders.WithContext;

import java.util.Optional;

public interface RequestMetadata
        extends WithContext<Metadata> {
    interface Builder<T> {
        Metadata.Builder builder();

        default T correlationId(String value) {
            builder().add("correlationId", value);
            return (T) this;
        }

        default T jwtClaims(ObjectNode value) {
            builder().add("jwtClaims", value);
            return (T) this;
        }

        default T remoteIp(String value) {
            builder().add("remoteIp", value);
            return (T) this;
        }

        default T agent(String value) {
            builder().add("agent", value);
            return (T) this;
        }
    }

    default Optional<String> getCorrelationId() {
        return context().getString("correlationId");
    }

    default Optional<ObjectNode> getJwtClaims() {
        return context().getJson("jwtClaims").map(ObjectNode.class::cast);
    }

    default Optional<String> getRemoteIp() {
        return context().getString("remoteIp");
    }

    default Optional<String> getAgent() {
        return context().getString("agent");
    }
}
