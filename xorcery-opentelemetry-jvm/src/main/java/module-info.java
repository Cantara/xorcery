module xorcery.opentelemetry.jvm {
    exports com.exoreaction.xorcery.opentelemetry.jvm;

    requires xorcery.configuration.api;

    requires io.opentelemetry.api;
    requires io.opentelemetry.semconv;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires java.management;
    requires xorcery.opentelemetry.api;
    requires jdk.management;
    requires org.apache.logging.log4j;
}