package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

public class KeyStoreSecretsProvider
        implements SecretsProvider {
    private final KeyStore keyStore;
    private final SecretKeyFactory factory;
    private final KeyStore.PasswordProtection keyStorePP;

    public KeyStoreSecretsProvider(KeyStore keyStore, char[] password) throws NoSuchAlgorithmException {
        this.keyStore = keyStore;
        factory = SecretKeyFactory.getInstance("PBE");
        keyStorePP = new KeyStore.PasswordProtection(password);
    }

    @Override
    public String getSecretString(String name) throws IOException {

        try {
            KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry(name, keyStorePP);
            PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
            char[] secret = keySpec.getPassword();
            return new String(secret);
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] getSecretBytes(String name) throws IOException {
        try {
            KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry(name, keyStorePP);
            PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
            char[] secret = keySpec.getPassword();
            return new String(secret).getBytes(StandardCharsets.UTF_8);
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    @Override
    public void refreshSecret(String name) throws IOException {
        // no-op
    }
}
