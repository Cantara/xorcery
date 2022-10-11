open module xorcery.conductor.api {
    exports com.exoreaction.xorcery.service.conductor.api;
    exports com.exoreaction.xorcery.service.conductor.helpers;

    requires transitive xorcery.config;
    requires transitive xorcery.jsonapi;
    requires transitive xorcery.service.api;
    requires xorcery.reactivestreams.api;

    requires jersey.common;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
}