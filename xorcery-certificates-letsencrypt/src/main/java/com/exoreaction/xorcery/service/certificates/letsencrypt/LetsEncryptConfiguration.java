package com.exoreaction.xorcery.service.certificates.letsencrypt;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Map;

public record LetsEncryptConfiguration(Configuration configuration) {

    public String getURL()
    {
        return configuration.getString("url").orElseThrow(()->new IllegalArgumentException("Missing letsencrypt.url"));
    }

    public URI getAccountLocation() {
        return configuration.getString("location").map(URI::create).orElseThrow(()->new IllegalArgumentException("Missing letsencrypt.location"));
    }

/*
    public Map<String, String> getCertificateLocations() {
        return configuration.getObjectAs("certificates", on -> JsonElement.toMap(on, JsonNode::textValue)).orElseThrow();
    }
*/
}
