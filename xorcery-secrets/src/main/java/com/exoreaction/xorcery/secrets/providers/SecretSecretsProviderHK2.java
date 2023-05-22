package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.secrets.spi.SecretsProvider;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service(name = "secret", metadata = "enabled=secrets.enabled")
@ContractsProvided(SecretsProvider.class)
public class SecretSecretsProviderHK2
        extends SecretSecretsProvider {
    @Inject
    public SecretSecretsProviderHK2() {
        super();
    }
}
