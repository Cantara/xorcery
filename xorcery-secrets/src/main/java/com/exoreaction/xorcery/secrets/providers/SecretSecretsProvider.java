package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Returns the name itself as the secret
 */
public class SecretSecretsProvider
        implements SecretsProvider {
    @Override
    public String getSecretString(String name) throws IOException {
        return name;
    }

    @Override
    public byte[] getSecretBytes(String name) throws IOException {
        return name.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void refreshSecret(String name) throws IOException {
        // No-op
    }
}
