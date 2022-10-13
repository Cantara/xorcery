open module xorcery.domainevents {
    exports com.exoreaction.xorcery.service.domainevents;
    exports com.exoreaction.xorcery.service.domainevents.api;
    exports com.exoreaction.xorcery.service.domainevents.api.aggregate;
    exports com.exoreaction.xorcery.service.domainevents.api.aggregate.annotation;
    exports com.exoreaction.xorcery.service.domainevents.api.context;
    exports com.exoreaction.xorcery.service.domainevents.resources;

    requires transitive xorcery.jsonapi.server;

    requires xorcery.metadata;
    requires xorcery.reactivestreams.api;
    requires xorcery.config.api;
    requires xorcery.conductor.api;
    requires xorcery.disruptor;
    requires xorcery.service.api;

    requires com.fasterxml.jackson.annotation;
    requires jersey.server;
    requires jersey.common;
    requires jakarta.validation;
    requires jakarta.inject;
}