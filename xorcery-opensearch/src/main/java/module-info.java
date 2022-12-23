open module xorcery.opensearch {
    requires xorcery.service.api;
    requires xorcery.configuration.api;
    requires xorcery.reactivestreams.api;
    requires xorcery.disruptor;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.util;

    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires java.logging;
    requires com.lmax.disruptor;
    requires org.glassfish.hk2.api;
}