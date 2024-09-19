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
package com.exoreaction.xorcery.jwt.server.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jwt.server.UserConfiguration;
import com.exoreaction.xorcery.jwt.server.spi.ClaimsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;

@Service(name = "jwt.server.claims.configuration")
@ContractsProvided(ClaimsProvider.class)
public class ConfigurationClaimsProvider
        implements ClaimsProvider {
    private final UserConfiguration users;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public ConfigurationClaimsProvider(Configuration configuration) {
        users = UserConfiguration.get(configuration);
    }

    @Override
    public Map<String, ?> getClaims(String userName) {

        return users.getUser(userName).map(node -> {
            try {
                return (Map<String, ?>)objectMapper.treeToValue(node.getClaims(), Map.class);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }).orElse(Collections.emptyMap());
    }
}
