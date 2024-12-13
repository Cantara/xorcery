module xorcery.opentelemetry.exporter.opensearch {
    exports dev.xorcery.opentelemetry.exporters.opensearch.attach;
    exports dev.xorcery.opentelemetry.exporters.reactivestreams;

    requires xorcery.reactivestreams.api;

    requires xorcery.opensearch.client;

    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.sdk.logs;

    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.exporter.internal.otlp;
    requires io.opentelemetry.exporter.internal;
    requires org.eclipse.jetty.util;
    requires jdk.unsupported;
}