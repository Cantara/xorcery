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
package dev.xorcery.configuration;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Configuration wrapper for application configuration
 *
 * @author rickardoberg
 */
public record ApplicationConfiguration(Configuration configuration) {
    public static ApplicationConfiguration get(Configuration configuration)
    {
        return new ApplicationConfiguration(configuration.getConfiguration("application"));
    }

    public String getName() {
        return configuration.getString("name").orElse(null);
    }

    public String getVersion() {
        return configuration.getString("version").orElse(null);
    }

    public List<String> getVersionPackages(){
        return configuration.getListAs("versions", JsonNode::textValue).orElse(List.of());
    }
}
