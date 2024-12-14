module xorcery.jetty.server.jwt {
    exports dev.xorcery.jetty.server.jwt;
    exports dev.xorcery.jetty.server.jwt.providers;

    requires com.auth0.jwt;
    requires org.bouncycastle.provider;
    requires xorcery.secrets.api;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires org.eclipse.jetty.security;
    requires org.glassfish.hk2.api;
    requires xorcery.configuration.api;

}