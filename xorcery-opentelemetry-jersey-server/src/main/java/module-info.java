/**
 * @author rickardoberg
 * @since 18/01/2024
 */

module xorcery.opentelemetry.jersey.server {
    exports com.exoreaction.xorcery.opentelemetry.jersey.server.resources;

    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    requires io.opentelemetry.semconv;
    requires xorcery.configuration.api;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires org.eclipse.jetty.server;
    requires jersey.server;
    requires jersey.common;
    requires xorcery.opentelemetry.api;

}