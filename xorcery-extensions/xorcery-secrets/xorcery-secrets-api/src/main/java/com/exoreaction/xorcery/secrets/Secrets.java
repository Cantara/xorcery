/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.secrets;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

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

    public void refreshSecret(String name) {
        getSecretsProvider(name, (provider, n) ->
        {
            provider.refreshSecret(n);
            return n;
        });
    }

    private <T> T getSecretsProvider(String secretName, BiFunction<SecretsProvider, String, T> function) {
        Objects.requireNonNull(secretName, "Secret name is null");

        String providerName;
        String[] names = secretName.split(":", 2);
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
