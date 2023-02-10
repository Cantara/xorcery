open module xorcery.eventstore {
    exports com.exoreaction.xorcery.service.eventstore;
    exports com.exoreaction.xorcery.service.eventstore.api;
    exports com.exoreaction.xorcery.service.eventstore.model;
    exports com.exoreaction.xorcery.service.eventstore.resources.api;

    requires xorcery.reactivestreams.api;
    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi.server;
    requires xorcery.util;

    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires db.client.java;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires com.lmax.disruptor;
    requires jersey.common;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires grpc.shaded.jpms;
}