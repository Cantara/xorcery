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
    requires com.lmax.disruptor;
    requires io.opentelemetry.api;
    requires xorcery.domainevents.api;
    exports com.exoreaction.xorcery.eventstore.api;
    exports com.exoreaction.xorcery.eventstore.streams;
    exports com.exoreaction.xorcery.eventstore.resources;
}