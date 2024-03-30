module xorcery.dns.update.routefiftythree {
    exports com.exoreaction.xorcery.dns.update.route53;

    requires xorcery.configuration.api;
    requires xorcery.dns.update;

    requires org.dnsjava;
    requires software.amazon.awssdk.services.route53;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.auth;
    requires xorcery.secrets.api;
    requires software.amazon.awssdk.awscore;
    requires software.amazon.awssdk.core;
}
