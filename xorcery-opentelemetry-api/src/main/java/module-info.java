module xorcery.opentelemetry.api {
    exports com.exoreaction.xorcery.opentelemetry;

    requires xorcery.configuration.api;
    requires transitive io.opentelemetry.api;
    requires io.opentelemetry.semconv;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}
