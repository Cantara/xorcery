package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.secrets.spi.SecretsProvider;
import com.exoreaction.xorcery.keystores.KeyStores;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service(name="keystore", metadata = "enabled=secrets.keystore.enabled")
@ContractsProvided(SecretsProvider.class)
public class KeyStoreSecretsProviderHK2
        extends KeyStoreSecretsProvider {
    @Inject
    public KeyStoreSecretsProviderHK2(Configuration configuration, KeyStores keyStores, Secrets secrets) throws NoSuchAlgorithmException, IOException {
        super(keyStores.getKeyStore(configuration.getString("secrets.keystore.name").orElseThrow(Configuration.missing("secrets.keystore.name"))),
                secrets.getSecretString(configuration.getString("secrets.keystore.password").orElseThrow(Configuration.missing("secrets.keystore.password"))).toCharArray());
    }
}
