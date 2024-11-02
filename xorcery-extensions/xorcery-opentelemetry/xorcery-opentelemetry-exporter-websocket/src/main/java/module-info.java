module xorcery.opentelemetry.exporter.websocket {
    exports dev.xorcery.opentelemetry.exporters.websocket.attach;
    exports dev.xorcery.opentelemetry.exporters.websocket.listen;
    exports dev.xorcery.opentelemetry.exporters.websocket;

    requires xorcery.reactivestreams.api;

    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.sdk.logs;

    requires org.apache.logging.log4j;
    requires java.logging;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.exporter.internal.otlp;
}