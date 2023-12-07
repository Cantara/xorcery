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
package com.exoreaction.xorcery.status.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.status.spi.StatusProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service(name = "status.configuration")
public class ConfigurationStatusProvider
        implements StatusProvider {

    private final Configuration configuration;

    @Inject
    public ConfigurationStatusProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getId() {
        return "configuration";
    }

    public void addAttributes(Attributes.Builder attributesBuilder, String filter) {
        ObjectNode configJson = configuration.json();
        String[] filterNames = filter.split(",");

        if (filter.isBlank()) {
            List<String> names = new ArrayList<>();
            configJson.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            for (String name : names) {
                attributesBuilder.attribute(name, configJson.get(name));
            }
        } else {
            configJson.fields().forEachRemaining(entry ->
            {
                for (String filterName : filterNames) {
                    if (entry.getKey().equals(filterName)) {
                        attributesBuilder.attribute(entry.getKey(), entry.getValue());
                        return;
                    }
                }
            });
        }
    }
}
