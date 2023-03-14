package com.exoreaction.xorcery.service.certificates.client;

import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

import java.util.Optional;

public interface CertificatesClientConfiguration
    extends ServiceConfiguration
{
    default Optional<String> getURI()
    {
        return context().getString("uri");
    }

    default boolean isRenewOnStartup()
    {
        return context().getBoolean("renewOnStartup").orElse(false);
    }
}
