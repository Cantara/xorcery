module xorcery.secrets.aws {

    exports dev.xorcery.secrets.aws.provider;

    requires xorcery.secrets.spi;
    requires xorcery.configuration.api;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.services.secretsmanager;
    requires software.amazon.awssdk.regions;
}