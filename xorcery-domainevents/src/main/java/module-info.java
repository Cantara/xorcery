open module xorcery.domainevents {
    exports com.exoreaction.xorcery.service.domainevents;
    exports com.exoreaction.xorcery.service.domainevents.api;
    exports com.exoreaction.xorcery.service.domainevents.api.entity;
    exports com.exoreaction.xorcery.service.domainevents.api.entity.annotation;
    exports com.exoreaction.xorcery.service.domainevents.api.context;
    exports com.exoreaction.xorcery.service.domainevents.api.model;
    exports com.exoreaction.xorcery.service.domainevents.resources;
    exports com.exoreaction.xorcery.service.domainevents.api.event;

    requires transitive xorcery.jsonapi.server;
    requires transitive xorcery.metadata;

    requires xorcery.reactivestreams.api;
    requires xorcery.configuration.api;

    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.annotation;
    requires jakarta.inject;
    requires jakarta.validation;
    requires org.glassfish.hk2.api;
}