package com.exoreaction.xorcery.service.certificates;

import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.exoreaction.xorcery.configuration.model.Configuration.missing;

public interface CertificatesConfiguration
        extends ServiceConfiguration {
    default Optional<String> getURI() {
        return context().getString("uri");
    }

    default String getSubject() {
        return context().getString("subject").orElseThrow(missing("certificates.subject"));
    }

    default boolean isRenewOnStartup() {
        return context().getBoolean("renewOnStartup").orElse(false);
    }

    default String getAlias() {
        return context().getString("alias").orElseThrow(missing("certificates.alias"));
    }

    default List<String> getIpAddresses()
    {
        return context().getListAs("ipAddresses", JsonNode::textValue).orElse(Collections.emptyList());
    }

    default List<String> getDnsNames()
    {
        return context().getListAs("dnsNames", JsonNode::textValue).orElse(Collections.emptyList());
    }
}
