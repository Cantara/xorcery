module xorcery.opentelemetry.jmx {
    exports com.exoreaction.xorcery.opentelemetry.jmx;

    requires xorcery.configuration.api;
    requires xorcery.opentelemetry.api;
    requires io.opentelemetry.semconv;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires java.management;
}