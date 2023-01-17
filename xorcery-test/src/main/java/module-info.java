open module xorcery.test {
    exports com.exoreaction.xorcery.service;
    exports com.exoreaction.xorcery.service.resources.api;

    requires xorcery.core;
    requires xorcery.service.api;
    requires xorcery.reactivestreams.api;
    requires xorcery.neo4j;
    requires jakarta.inject;
    requires org.glassfish.hk2.runlevel;
    requires org.glassfish.hk2.api;
    requires xorcery.keystores;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.server;
    requires jjwt.api;
    requires xorcery.server;
}