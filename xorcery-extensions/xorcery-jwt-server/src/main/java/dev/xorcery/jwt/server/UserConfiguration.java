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
package dev.xorcery.jwt.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public record UserConfiguration(List<ConfigurationUser> users) {
    public static UserConfiguration get(Configuration configuration)
    {
        return new UserConfiguration(configuration.getObjectListAs("jwt.users",ConfigurationUser::new).orElse(Collections.emptyList()));
    }

    public Optional<ConfigurationUser> getUser(String username) {
        for (ConfigurationUser user : users) {
            if (Objects.equals(user.getName(), username))
                return Optional.of(user);
        }
        return Optional.empty();
    }

    public record ConfigurationUser(JsonNode json)
            implements JsonElement {

        public String getName()
        {
            return getString("name").orElse(null);
        }

        public Optional<String> getPassword()
        {
            return getString("password");
        }

        public ObjectNode getClaims()
        {
            return getObjectAs("claims", Function.identity()).orElse(JsonNodeFactory.instance.objectNode());
        }
    }
}
