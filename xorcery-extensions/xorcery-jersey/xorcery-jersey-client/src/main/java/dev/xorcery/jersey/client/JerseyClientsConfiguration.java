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
package dev.xorcery.jersey.client;

import dev.xorcery.configuration.Configuration;

import java.util.List;
import java.util.Optional;

public record JerseyClientsConfiguration(Configuration context) {
    public List<JerseyClientConfiguration> getClients() {
        return context.getObjectListAs("clients", on -> new JerseyClientConfiguration(new Configuration(on))).orElse(List.of());
    }

    public Optional<JerseyClientConfiguration> getClient(String name){
        return getClients().stream().filter(config -> name.equals(config.getName())).findFirst();
    }

}
