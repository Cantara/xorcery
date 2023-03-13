package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.model.Configuration;

public record KeyStoresConfiguration(Configuration configuration) {

    public boolean isEnabled() {
        return configuration.getBoolean("enabled").orElse(false);
    }

    public KeyStoreConfiguration getKeyStoreConfiguration(String name)
    {
        return new KeyStoreConfiguration(name, configuration.getConfiguration(name));
    }
}
