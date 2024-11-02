module xorcery.secrets.spi {
    exports dev.xorcery.secrets.spi;
    exports dev.xorcery.secrets.providers;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;

    provides SecretsProvider with EnvSecretsProvider, SecretSecretsProvider, SystemPropertiesSecretsProvider;
}