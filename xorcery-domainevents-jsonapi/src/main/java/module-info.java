open module xorcery.domainevents.jsonapi {
    exports com.exoreaction.xorcery.domainevents.jsonapi.resources;

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
    requires xorcery.domainevents.publisher;
}