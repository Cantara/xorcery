open module xorcery.conductor.api {
    requires xorcery.config;
    requires xorcery.jsonapi;
    requires xorcery.service.api;
    requires xorcery.registry;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires jersey.common;
    requires commons.jexl3;
    requires xorcery.reactivestreams.api;
    exports com.exoreaction.xorcery.service.conductor.api;
    exports com.exoreaction.xorcery.service.reactivestreams.helper;
    exports com.exoreaction.xorcery.service.conductor.resources.model;
}