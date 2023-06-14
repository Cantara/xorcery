package com.exoreaction.xorcery.secrets.spi;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public interface SecretsProvider {

    default String getSecretString(String name)
            throws UncheckedIOException, IllegalArgumentException
    {
        return new String(getSecretBytes(name), StandardCharsets.UTF_8);
    }

    default byte[] getSecretBytes(String name)
            throws UncheckedIOException, IllegalArgumentException
    {
        return getSecretString(name).getBytes(StandardCharsets.UTF_8);
    }

    default void refreshSecret(String name)
            throws UncheckedIOException, IllegalArgumentException
    {
    }
}
