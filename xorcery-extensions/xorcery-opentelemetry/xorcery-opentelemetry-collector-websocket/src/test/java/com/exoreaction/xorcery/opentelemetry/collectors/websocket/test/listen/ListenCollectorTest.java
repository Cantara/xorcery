package dev.xorcery.opentelemetry.collectors.websocket.test.listen;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.opentelemetry.collectors.websocket.WebsocketCollectorService;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.BaseSubscriber;

import java.nio.charset.StandardCharsets;

@Disabled
public class ListenCollectorTest {

    @RegisterExtension
    @Order(1)
    static XorceryExtension collector = XorceryExtension.xorcery()
            .id("collector")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    jetty.server.ssl.port: "{{ CALCULATED.dynamicPorts.ssl }}"
                    opentelemetry.exporters.websocket.enabled: false
                    """)
            .build();

    @RegisterExtension
    @Order(2)
    static XorceryExtension exporter = XorceryExtension.xorcery()
            .id("exporter")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml(String.format("""
                    opentelemetry.exporters.websocket.attach.host: "wss://localhost:%d/"
                    opentelemetry.exporters.websocket.attach.optimizeResource: true
                    opentelemetry.exporters.websocket.metrics.enabled: true
                    opentelemetry.exporters.websocket.metrics.interval: 2s
                    opentelemetry.exporters.websocket.logging.enabled: false
                    opentelemetry.exporters.logging.enabled: false
                    """, collector.getConfiguration().getInteger("jetty.server.ssl.port").orElseThrow()))
            .build();

    @Test
    public void testListenCollector() throws InterruptedException {
        WebsocketCollectorService collectorService = collector.getServiceLocator().getService(WebsocketCollectorService.class);
        collectorService.subscribeMetrics(new BaseSubscriber<>() {
            @Override
            protected void hookOnNext(MetadataByteBuffer value) {
                System.out.println("Metric:" + new String(value.data().array(), StandardCharsets.UTF_8));
            }
        });
        collectorService.subscribeLogs(new BaseSubscriber<>() {
            @Override
            protected void hookOnNext(MetadataByteBuffer value) {
                System.out.println("Log:" + new String(value.data().array(), StandardCharsets.UTF_8));
            }
        });
        collectorService.subscribeTraces(new BaseSubscriber<>() {
            @Override
            protected void hookOnNext(MetadataByteBuffer value) {
                System.out.println("Trace:" + new String(value.data().array(), StandardCharsets.UTF_8));
            }
        });

        exporter.getServiceLocator().getService(LoggerContext.class).getLogger("test").info("Test message");
        Span span = exporter.getServiceLocator().getService(OpenTelemetry.class).getTracer(getClass().getName())
                .spanBuilder("test span")
                .setAttribute("someattr", "somevalue")
                .startSpan();
        span.addEvent("someevent");

        Thread.sleep(10000);
        span.end();
    }
}
