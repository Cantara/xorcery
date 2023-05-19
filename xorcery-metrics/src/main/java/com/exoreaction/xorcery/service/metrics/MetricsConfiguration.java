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
package com.exoreaction.xorcery.service.metrics;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

public record MetricsConfiguration(Configuration context)
        implements ServiceConfiguration {
    public String getSubscriberAuthority() {
        return context.getString("subscriber.authority").orElse(null);
    }

    public String getSubscriberStream() {
        return context.getString("subscriber.stream").orElseThrow();
    }

    public Configuration getSubscriberConfiguration() {
        return context.getConfiguration("subscriber.configuration");
    }

    public Configuration getPublisherConfiguration() {
        return context.getConfiguration("publisher.configuration");
    }

    public Optional<Iterable<JsonNode>> getFilters() {
        return context.getList("filter");
    }
}
