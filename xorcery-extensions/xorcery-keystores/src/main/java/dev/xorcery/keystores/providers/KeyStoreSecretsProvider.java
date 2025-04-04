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
package dev.xorcery.keystores.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.keystores.KeyStoreConfiguration;
import dev.xorcery.keystores.KeyStores;
import dev.xorcery.keystores.KeyStoresConfiguration;
import dev.xorcery.secrets.Secrets;
import dev.xorcery.secrets.spi.SecretsProvider;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

public class KeyStoreSecretsProvider
        implements SecretsProvider {
    private final String keyStoreName;
    private final KeyStore keyStore;
    private final SecretKeyFactory factory;
    private final KeyStore.PasswordProtection keyStorePP;

    public KeyStoreSecretsProvider(Configuration configuration, KeyStores keyStores, Secrets secrets) throws NoSuchAlgorithmException, IOException {
        KeyStoreSecretsProviderConfiguration config = new KeyStoreSecretsProviderConfiguration(configuration.getConfiguration("secrets.keystore"));
        keyStoreName = config.getKeyStoreName();
        this.keyStore = keyStores.getKeyStore(config.getKeyStoreName());
        factory = SecretKeyFactory.getInstance("PBE");
        KeyStoreConfiguration keyStoreConfiguration = KeyStoresConfiguration.get(configuration).getKeyStoreConfiguration(config.getKeyStoreName()).orElseThrow(Configuration.missing(config.getKeyStoreName()));
        char[] password = keyStoreConfiguration.getPassword().map(secrets::getSecretString).map(String::toCharArray).orElse(null);
        keyStorePP = new KeyStore.PasswordProtection(password);
    }

    @Override
    public String getSecretString(String name) {

        try {
            KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry(name, keyStorePP);
            if (ske == null)
                throw new IllegalArgumentException("No secret with alias '" + name + "' in keystore '" + keyStoreName + "'");
            PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
            char[] secret = keySpec.getPassword();
            return new String(secret);
        } catch (Throwable e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public byte[] getSecretBytes(String name) {
        try {
            KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry(name, keyStorePP);
            if (ske == null)
                throw new IllegalArgumentException("No secret with alias '" + name + "' in keystore '" + keyStoreName + "'");
            PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
            char[] secret = keySpec.getPassword();
            return new String(secret).getBytes(StandardCharsets.UTF_8);
        } catch (Throwable e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public void refreshSecret(String name) {
        // no-op
    }
}
