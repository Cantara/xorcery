module xorcery.opentelemetry.sdk {

    exports com.exoreaction.xorcery.opentelemetry.sdk;
    exports com.exoreaction.xorcery.opentelemetry.sdk.exporters;
    exports com.exoreaction.xorcery.opentelemetry.sdk.exporters.logging;
    exports com.exoreaction.xorcery.opentelemetry.sdk.exporters.otlphttp;
    exports com.exoreaction.xorcery.opentelemetry.sdk.exporters.otlphttp.jdk;

    requires xorcery.configuration.api;
    requires xorcery.secrets;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;

    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.logs;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.context;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.semconv;
    requires io.opentelemetry.exporter.logging;
    requires io.opentelemetry.exporter.otlp;
    requires io.opentelemetry.exporter.internal;
    requires jakarta.annotation;
    requires java.net.http;

    provides io.opentelemetry.exporter.internal.http.HttpSenderProvider with com.exoreaction.xorcery.opentelemetry.sdk.exporters.otlphttp.jdk.JdkHttpSenderProvider;
}