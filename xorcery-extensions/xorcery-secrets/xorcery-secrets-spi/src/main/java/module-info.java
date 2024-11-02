module xorcery.secrets.spi {
    exports dev.xorcery.secrets.spi;
    exports dev.xorcery.secrets.providers;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;

    provides dev.xorcery.secrets.spi.SecretsProvider with
            dev.xorcery.secrets.providers.EnvSecretsProvider,
            dev.xorcery.secrets.providers.SecretSecretsProvider,
            dev.xorcery.secrets.providers.SystemPropertiesSecretsProvider;
}