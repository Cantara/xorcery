package com.exoreaction.xorcery.secrets;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Lookup secrets from secret providers.
 * <p>
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
        return getSecretsProvider(name, SecretsProvider::getSecretString);
    }

    public byte[] getSecretBytes(String name)
            throws UncheckedIOException, IllegalArgumentException {
        return getSecretsProvider(name, SecretsProvider::getSecretBytes);
    }

    public void refreshSecret(String name)
            throws IOException {
        getSecretsProvider(name, (provider, n) ->
        {
            provider.refreshSecret(n);
            return n;
        });
    }

    private <T> T getSecretsProvider(String secretName, BiFunction<SecretsProvider, String, T> function) {
        Objects.requireNonNull(secretName, "Secret name is null");

        String providerName;
        String[] names = secretName.split(":");
        if (names.length == 2) {
            providerName = names[0];
            secretName = names[1];
        } else {
            providerName = defaultSecretsProviderName;
        }
        return function.apply(Optional.ofNullable(providers.apply(providerName))
                .orElseThrow(() -> new IllegalArgumentException("No secrets provider available named " + providerName)), secretName);
    }
}
