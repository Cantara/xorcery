open module xorcery.opensearch {
    requires xorcery.conductor.api;
    requires xorcery.reactivestreams.api;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.server;
    requires jersey.client;
    requires jersey.jetty.connector;
    requires jersey.server;
    requires jakarta.inject;
    requires java.logging;
    requires com.lmax.disruptor;
    requires xorcery.disruptor;
    requires xorcery.domainevents;
    requires xorcery.restclient;
    requires xorcery.jsonapi.jaxrs;
}