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
    requires xorcery.service.api;
    requires com.lmax.disruptor;
    exports com.exoreaction.xorcery.eventstore.api;
    exports com.exoreaction.xorcery.eventstore.streams;
    exports com.exoreaction.xorcery.eventstore.resources;
}