module xorcery.aws.auth {
    exports dev.xorcery.aws.auth;

    requires xorcery.configuration.api;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires software.amazon.awssdk.auth;
    requires xorcery.secrets.api;
    requires software.amazon.awssdk.profiles;
}