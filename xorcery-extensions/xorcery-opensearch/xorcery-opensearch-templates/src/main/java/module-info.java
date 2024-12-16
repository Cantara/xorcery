module xorcery.opensearch.templates {
    exports dev.xorcery.opensearch.templates;
    opens opensearch.templates;
    opens opensearch.templates.components;

    requires xorcery.opensearch.client;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires io.opentelemetry.api;
    requires xorcery.reactivestreams.api;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}