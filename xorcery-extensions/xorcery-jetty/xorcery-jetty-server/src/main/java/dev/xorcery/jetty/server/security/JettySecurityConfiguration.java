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
package dev.xorcery.jetty.server.security;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record JettySecurityConfiguration(Configuration configuration) {

    public static JettySecurityConfiguration get(Configuration configuration) {
        return new JettySecurityConfiguration(configuration.getConfiguration("jetty.server.security"));
    }

    public Optional<String> getAuthenticationType() {
        return configuration.getString("type")
                .or(()->configuration.getString("method")); // Deprecated
    }

    public List<ConstraintConfiguration> getConstraints() {
        return configuration.getObjectListAs("constraints", on -> new ConstraintConfiguration(new Configuration(on)))
                .orElse(Collections.emptyList());
    }

    public List<MappingConfiguration> getMappings() {
        return configuration.getObjectListAs("mappings", on -> new MappingConfiguration(new Configuration(on)))
                .orElse(Collections.emptyList());
    }


    public record ConstraintConfiguration(Configuration configuration) {
        public String getName() {
            return configuration.getString("name").orElseThrow(Configuration.missing("name"));
        }

        public List<String> getRoles() {
            return configuration.getListAs("roles", JsonNode::textValue)
                    .orElse(Collections.emptyList());
        }

    }

    public record MappingConfiguration(Configuration configuration) {
        public String getPath() {
            return configuration.getString("path").orElseThrow(Configuration.missing("path"));
        }

        public Optional<String> getConstraint() {
            return configuration.getString("constraint");
        }
    }
}
