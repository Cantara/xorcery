open module xorcery.jetty.server.jwt.test {
    requires xorcery.configuration;
    requires xorcery.jetty.server.jwt;
    requires xorcery.core;
    requires xorcery.jsonapi.api;

    requires com.auth0.jwt;
    requires jakarta.ws.rs;
    requires org.junit.jupiter.api;
    requires org.glassfish.hk2.api;
}