package com.exoreaction.xorcery.secrets;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Lookup secrets from secret providers.
 *
 * Name can either be name of the secret in the default provider, or of the format:
 * provider:name
 * such as:
 * env:mysecretenvironmentvariable
 */
public class Secrets {

    private final Function<String, SecretsProvider> providers;
    private final String defaultSecretsProviderName;

    public Secrets(Function<String, SecretsProvider> providers, String defaultSecretsProviderName) {
        this.providers = providers;
        this.defaultSecretsProviderName = defaultSecretsProviderName;
    }

    public String getSecretString(String name)
            throws UncheckedIOException, IllegalArgumentException {

        Objects.requireNonNull(name, "Secret name is null");

        String providerName;
        String[] names = name.split(":");
        if (names.length == 2) {
            providerName = names[0];
            name = names[1];
        } else {
            providerName = defaultSecretsProviderName;
        }
        return Optional.ofNullable(providers.apply(providerName))
                .orElseThrow(() -> new IllegalArgumentException("No secrets provider available named " + providerName))
                .getSecretString(name);
    }

    public byte[] getByteSecret(String name)
            throws UncheckedIOException, IllegalArgumentException {
        String providerName;
        String[] names = name.split(":");
        if (names.length == 2) {
            providerName = names[0];
            name = names[1];
        } else {
            providerName = defaultSecretsProviderName;
        }
        return Optional.ofNullable(providers.apply(providerName)).
                orElseThrow(() -> new IllegalArgumentException("No secrets provider available named " + providerName))
                .getSecretBytes(name);

    }

    public void refreshSecret(String name)
            throws IOException {
        String providerName;
        String[] names = name.split(":");
        if (names.length == 2) {
            providerName = names[0];
            name = names[1];
        } else {
            providerName = defaultSecretsProviderName;
        }
        Optional.ofNullable(providers.apply(providerName)).
                orElseThrow(() -> new IllegalArgumentException("No secrets provider available named " + providerName))
                .refreshSecret(name);
    }
}
