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
package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record DnsClientConfiguration(Configuration context)
        implements ServiceConfiguration {
    public ObjectNode getHosts() {
        return (ObjectNode) context().getJson("hosts").orElseGet(JsonNodeFactory.instance::objectNode);
    }

    public Optional<List<String>> getNameServers() {
        return context().getListAs("nameServers", JsonNode::textValue);
    }

    public Duration getTimeout() {
        return Duration.parse("PT" + context().getString("timeout").orElse("30s"));
    }

    public List<Name> getSearchDomains() {
        return context().getListAs("search", json -> {
            try {
                return Name.fromString(json.textValue());
            } catch (TextParseException e) {
                throw new UncheckedIOException(e);
            }
        }).orElseGet(Collections::emptyList);
    }
}
