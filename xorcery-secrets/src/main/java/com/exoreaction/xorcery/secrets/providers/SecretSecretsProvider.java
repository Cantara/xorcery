package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

import java.nio.charset.StandardCharsets;

/**
 * Returns the name itself as the secret
 */
public class SecretSecretsProvider
        implements SecretsProvider {
    @Override
    public String getSecretString(String name) {
        return name;
    }

    @Override
    public byte[] getSecretBytes(String name) {
        return name.getBytes(StandardCharsets.UTF_8);
    }
}
