module xorcery.secrets.aws {

    exports com.exoreaction.xorcery.secrets.aws.provider;

    requires xorcery.secrets;
    requires xorcery.configuration.api;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.services.secretsmanager;
    requires software.amazon.awssdk.regions;
}