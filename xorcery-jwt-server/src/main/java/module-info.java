open module xorcery.jwt.jsonapi {

    requires transitive xorcery.jsonapi.server;
    requires transitive xorcery.metadata;

    requires xorcery.configuration.api;

    requires org.eclipse.jetty.security;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.annotation;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.ws.rs;
    requires xorcery.domainevents.jsonapi;
    requires xorcery.jetty.server;
    requires jjwt.api;
    requires xorcery.service.api;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
}