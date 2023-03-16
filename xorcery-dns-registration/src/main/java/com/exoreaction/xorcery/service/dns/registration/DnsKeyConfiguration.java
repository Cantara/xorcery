package com.exoreaction.xorcery.service.dns.registration;

import com.exoreaction.xorcery.configuration.model.Configuration;

public record DnsKeyConfiguration(Configuration configuration) {
    public String getName() {
        return configuration.getString("name").orElseThrow(() -> new IllegalArgumentException("Name missing"));
    }

    public String getSecret() {
        return configuration.getString("secret").orElseThrow(() -> new IllegalArgumentException("Secret missing"));
    }

    public String getAlgorithm() {
        return configuration.getString("algorithm").orElse("HMAC-MD5.SIG-ALG.REG.INT.");
    }
}
