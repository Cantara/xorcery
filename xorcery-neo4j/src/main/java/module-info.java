open module xorcery.neo4j {
    exports com.exoreaction.xorcery.service.neo4j.client;
    exports com.exoreaction.xorcery.service.neo4j;
    exports com.exoreaction.xorcery.neo4j.jsonapi.resources;
    exports com.exoreaction.xorcery.service.neo4jprojections.aggregate;
    exports com.exoreaction.xorcery.service.neo4jprojections.api;

    requires transitive xorcery.jsonapi.server;
    requires transitive xorcery.neo4j.shaded;

    requires xorcery.domainevents;
    requires xorcery.metadata;
//    requires xorcery.registry;
    requires xorcery.jsonschema;
    requires xorcery.util;
    requires xorcery.service.api;
    requires xorcery.config.api;
    requires xorcery.json;
    requires xorcery.reactivestreams.api;
    requires xorcery.conductor.api;
    requires xorcery.disruptor;

    requires org.apache.logging.log4j;
    requires jakarta.validation;
    requires org.apache.commons.lang3;
    requires jersey.common;
    requires jakarta.inject;
    requires jersey.server;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.codahale.metrics;
    requires com.lmax.disruptor;
    requires xorcery.jsonapi;
//    requires org.neo4j.graphdb;
//    requires org.neo4j.kernel;
}