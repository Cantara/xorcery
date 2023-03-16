open module xorcery.jmxmetrics {
    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.reactivestreams.api;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.jaxrs;

    requires com.codahale.metrics;
    requires com.fasterxml.jackson.databind;
    requires com.codahale.metrics.jmx;
    requires com.lmax.disruptor;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires org.apache.logging.log4j;
    requires java.management;
    requires java.logging;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
}