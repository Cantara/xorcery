package com.exoreaction.xorcery.secrets.providers;

import com.exoreaction.xorcery.secrets.spi.SecretsProvider;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name="env", metadata = "enabled=secrets.env.enabled")
@ContractsProvided(SecretsProvider.class)
public class EnvSecretsProviderHK2
    extends EnvSecretsProvider
{

    @Inject
    public EnvSecretsProviderHK2() {
    }
}
