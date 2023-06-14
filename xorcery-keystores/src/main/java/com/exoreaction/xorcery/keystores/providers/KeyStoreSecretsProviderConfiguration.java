package com.exoreaction.xorcery.keystores.providers;

import com.exoreaction.xorcery.configuration.Configuration;

public record KeyStoreSecretsProviderConfiguration(Configuration configuration) {

    public String getKeyStoreName()
    {
        return configuration.getString("name").orElseThrow(Configuration.missing("secrets.keystore.name"));
    }
}
