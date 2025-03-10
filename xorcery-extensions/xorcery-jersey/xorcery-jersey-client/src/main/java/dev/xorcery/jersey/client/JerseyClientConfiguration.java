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

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;

import java.time.Duration;
import java.util.Optional;

public record JerseyClientConfiguration(Configuration configuration) {
    public Optional<JsonNode> getProperties() {
        return configuration.getJson("properties");
    }

    public Duration getConnectTimeout() {
        return Duration.parse("PT" + configuration().getString("connectTimeout").orElse("5s"));
    }

    public Duration getReadTimeout() {
        return Duration.parse("PT" + configuration().getString("idleTimeout").orElse("0s"));
    }

    public Optional<String> getKeyStoreName()
    {
        return configuration.getString("keystore");
    }

    public Optional<String> getTrustStoreName()
    {
        return configuration.getString("truststore");
    }
}
