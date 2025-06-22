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
package dev.xorcery.reactivestreams.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;

import javax.net.ssl.SSLHandshakeException;
import java.util.Collections;
import java.util.List;

public record ClientConfiguration(Configuration configuration) {

    public static ClientConfiguration defaults()
    {
        return new ClientConfiguration(Configuration.newConfiguration().build());
    }

    public record Builder(Configuration.Builder builder) {
        public Builder() {
            this(Configuration.newConfiguration());
        }

        public Builder isRetryEnabled(boolean value) {
            builder.add("retry.enabled", value);
            return this;
        }

        public Builder retryDelay(String duration) {
            builder.add("retry.duration", duration);
            return this;
        }

        public Builder extensions(String... websocketExtensionName)
        {
            builder.add("websocket.extensions", websocketExtensionName);
            return this;
        }

        public ClientConfiguration build() {
            return new ClientConfiguration(builder.build());
        }
    }

    public boolean isRetryEnabled() {
        return configuration.getBoolean("retry.enabled").orElse(true);
    }

    public boolean isRetryable(Throwable throwable)
    {
        if (throwable == null)
            return true;

        if (throwable instanceof SSLHandshakeException) {
            return false;
        }

        // TODO Allow includes and excludes in config
        return true;
    }

    public String getRetryDelay() {
        return configuration.getString("retry.delay").orElse("10S");
    }

    public List<String> getExtensions() {
        return configuration.getListAs("websocket.extensions", JsonNode::textValue).orElse(Collections.emptyList());
    }
}
