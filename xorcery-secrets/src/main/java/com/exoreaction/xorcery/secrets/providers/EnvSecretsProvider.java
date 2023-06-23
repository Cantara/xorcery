package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class EnvSecretsProvider
        implements SecretsProvider {
    @Override
    public String getSecretString(String name) {
        return Optional.ofNullable(System.getenv(name))
                .orElseThrow(() -> new IllegalArgumentException("No such environment variable:" + name));
    }
}
