module xorcery.jaxrs.server {
    exports com.exoreaction.xorcery.jaxrs.server.resources;
    opens com.exoreaction.xorcery.jaxrs.server.resources;

    requires jakarta.ws.rs;
    requires jetty.servlet.api;
    requires org.glassfish.hk2.api;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.security;
    requires jakarta.inject;
}