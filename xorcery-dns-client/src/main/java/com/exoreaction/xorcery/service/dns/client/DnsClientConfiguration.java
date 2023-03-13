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
