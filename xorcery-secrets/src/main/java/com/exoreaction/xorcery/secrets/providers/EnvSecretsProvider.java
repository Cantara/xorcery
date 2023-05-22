package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EnvSecretsProvider
    implements SecretsProvider
{
    @Override
    public String getSecretString(String name) throws IOException {
        return System.getenv(name);
    }

    @Override
    public byte[] getSecretBytes(String name) throws IOException {
        return System.getenv(name).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void refreshSecret(String name) throws IOException {
        // No-op
    }
}
