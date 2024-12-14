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
package dev.xorcery.jetty.server;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public record JettyServerConfiguration(Configuration configuration) {

    public static JettyServerConfiguration get(Configuration configuration) {
        return new JettyServerConfiguration(configuration.getConfiguration("jetty.server"));
    }

    public Duration getIdleTimeout() {
        return Duration.parse("PT" + configuration.getString("idleTimeout").orElse("-1s"));
    }

    public boolean isHttpEnabled() {
        return configuration.getBoolean("http.enabled").orElse(true);
    }

    public int getHttpPort() {
        return configuration.getInteger("http.port").orElse(8889);
    }

    public int getMinThreads() {
        return configuration.getInteger("minThreads").orElse(10);
    }

    public int getMaxThreads() {
        return configuration.getInteger("maxThreads").orElse(150);
    }

    public int getOutputBufferSize() {
        return configuration.getInteger("outputBufferSize").orElse(32768);
    }

    public int getRequestHeaderSize() {
        return configuration.getInteger("requestHeaderSize").orElse(16384);
    }

    public Optional<Map<String, String>> getMediaTypes() {
        return configuration.getObjectAs("mediaTypes", objectNode -> JsonElement.toMap(objectNode, JsonNode::textValue));
    }
}
