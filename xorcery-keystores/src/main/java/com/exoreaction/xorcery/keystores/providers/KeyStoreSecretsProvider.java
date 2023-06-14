package com.exoreaction.xorcery.keystores.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.keystores.KeyStoreConfiguration;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

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
        KeyStoreConfiguration keyStoreConfiguration = new KeyStoreConfiguration(config.getKeyStoreName(), configuration.getConfiguration("keystores." + config.getKeyStoreName()));
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
