open module xorcery.conductor {
    exports com.exoreaction.xorcery.service.conductor;

    requires transitive xorcery.jsonapi;
    requires transitive xorcery.conductor.api;
    requires transitive xorcery.configuration.api;
    requires transitive xorcery.service.api;
    requires xorcery.core;
    requires xorcery.jsonapi.server;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires com.fasterxml.jackson.core;
    requires org.apache.logging.log4j;
    requires commons.jexl3;
    requires com.fasterxml.jackson.dataformat.yaml;
}