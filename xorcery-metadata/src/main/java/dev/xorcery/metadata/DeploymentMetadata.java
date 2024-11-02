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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.xorcery.builders.WithContext;
import dev.xorcery.configuration.ApplicationConfiguration;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface DeploymentMetadata
        extends WithContext<Metadata> {

    interface Builder<T> {
        Metadata.Builder builder();

        default T configuration(Configuration configuration) {
            InstanceConfiguration instanceConfiguration = InstanceConfiguration.get(configuration);
            ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.get(configuration);
            builder().add("environment", instanceConfiguration.getEnvironment())
                    .add("tags", Optional.ofNullable(instanceConfiguration.configuration().json().get("tags"))
                            .map(ArrayNode.class::cast)
                            .orElseGet(JsonNodeFactory.instance::arrayNode))
                    .add("name", applicationConfiguration.getName())
                    .add("version", applicationConfiguration.getVersion())
                    .add("host", instanceConfiguration.getHost());
            return (T) this;
        }
    }

    default String getEnvironment() {
        return context().getString("environment").orElse("default");
    }

    default List<String> getTags() {
        return context().getListAs("tags", JsonNode::textValue).orElse(Collections.emptyList());
    }

    default String getVersion() {
        return context().getString("version").orElse("0.1.0");
    }

    default String getName() {
        return context().getString("name").orElse("noname");
    }

    default String getHost() {
        return context().getString("host").orElse("localhost");
    }
}
