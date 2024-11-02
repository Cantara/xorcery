module xorcery.jaxrs.server {
    exports dev.xorcery.jaxrs.server.resources;
    opens dev.xorcery.jaxrs.server.resources;

    requires jakarta.ws.rs;
    requires jakarta.servlet;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.security;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}