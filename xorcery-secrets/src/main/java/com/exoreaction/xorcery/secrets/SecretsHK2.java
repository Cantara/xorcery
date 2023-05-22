package com.exoreaction.xorcery.secrets;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.secrets.spi.SecretsProvider;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name="secrets")
@ContractsProvided(Secrets.class)
public class SecretsHK2
    extends Secrets
{
    @Inject
    public SecretsHK2(IterableProvider<SecretsProvider> providers, Configuration configuration) {
        super(name -> providers.named(name).get(), configuration.getString("secrets.default").orElse("env"));
    }
}
