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
package dev.xorcery.jetty.client;

import dev.xorcery.configuration.Configuration;

import java.util.Optional;

public record JettyClientSslConfiguration(Configuration configuration) {

    public boolean isEnabled() {
        return configuration.getBoolean("enabled").orElse(false);
    }

    public Optional<String> getKeyStoreName() {
        return configuration.getString("keystore");
    }

    public Optional<String> getTrustStoreName() {
        return configuration.getString("truststore");
    }

    public String getAlias() {
        return configuration.getString("alias").orElse("self");
    }

    public String getEndpointIdentificationAlgorithm() {
        return configuration.getString("endpointIdentificationAlgorithm").orElse("HTTPS");
    }

    public boolean isTrustAll() {
        return configuration.getBoolean("trustAll").orElse(false);
    }
}
