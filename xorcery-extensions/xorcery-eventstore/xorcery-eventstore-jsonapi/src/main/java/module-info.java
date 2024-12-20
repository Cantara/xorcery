module xorcery.eventstore.jsonapi {
    requires xorcery.metadata;
    requires xorcery.jsonapi.server;
    requires xorcery.jaxrs.server;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires org.glassfish.hk2.api;
    requires db.client.java;
    requires xorcery.eventstore.client;
    requires xorcery.reactivestreams.api;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires xorcery.jsonapi.service;
    requires io.opentelemetry.api;
    requires xorcery.domainevents.api;
    exports dev.xorcery.eventstore.api;
    exports dev.xorcery.eventstore.streams;
    exports dev.xorcery.eventstore.resources;
}