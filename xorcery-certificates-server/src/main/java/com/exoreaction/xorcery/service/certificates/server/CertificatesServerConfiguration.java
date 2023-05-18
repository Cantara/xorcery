package com.exoreaction.xorcery.service.certificates.server;

import com.exoreaction.xorcery.configuration.model.Configuration;

public record CertificatesServerConfiguration(Configuration configuration) {

    public Authorization getAuthorizationType() {
        return configuration.getEnum("authorization", Authorization.class).orElse(Authorization.none);
    }

    public enum Authorization {
        none,
        provisioning,
        dns,
        ip
    }
}
