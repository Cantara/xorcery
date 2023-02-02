open module xorcery.domainevents.publisher {
    exports com.exoreaction.xorcery.domainevents.helpers.entity;
    exports com.exoreaction.xorcery.domainevents.helpers.entity.annotation;
    exports com.exoreaction.xorcery.domainevents.helpers.context;
    exports com.exoreaction.xorcery.domainevents.helpers.model;
    exports com.exoreaction.xorcery.domainevents.publisher;

    requires transitive xorcery.jsonapi.server;
    requires transitive xorcery.metadata;

    requires xorcery.reactivestreams.api;
    requires xorcery.configuration.api;

    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.annotation;
    requires jakarta.inject;
    requires jakarta.validation;
    requires org.glassfish.hk2.api;
    requires xorcery.domainevents;
}