open module xorcery.domainevents {
    requires xorcery.metadata;
    requires com.fasterxml.jackson.annotation;
    requires jakarta.ws.rs;
    requires jersey.common;
    requires xorcery.reactivestreams.api;
    requires xorcery.util;
    requires xorcery.config;
    requires xorcery.conductor.api;
    requires xorcery.disruptor;
    requires xorcery.service.api;
    requires com.lmax.disruptor;
    requires jakarta.inject;
    requires jersey.server;
    requires xorcery.jsonapi;
    exports com.exoreaction.xorcery.service.domainevents.api;
    exports com.exoreaction.xorcery.service.domainevents.api.aggregate;
    exports com.exoreaction.xorcery.service.domainevents.api.context;
    exports com.exoreaction.xorcery.service.domainevents.api.aggregate.annotation;
}