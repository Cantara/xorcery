package com.exoreaction.xorcery.opentelemetry.collectors.websocket.test.listen;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.opentelemetry.collectors.websocket.WebsocketCollectorService;
import com.exoreaction.xorcery.opentelemetry.collectors.websocket.attach.AttachCollectorService;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;

import java.nio.charset.StandardCharsets;

@Disabled
public class AttachCollectorTest {

    @RegisterExtension
    @Order(1)
    static XorceryExtension collector = XorceryExtension.xorcery()
            .id("collector")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    opentelemetry.exporters.websocket.enabled: false
                    opentelemetry.exporters.logging.enabled: false
                    """)
            .build();

    @RegisterExtension
    @Order(2)
    static XorceryExtension exporter = XorceryExtension.xorcery()
            .id("exporter")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    jetty.server.ssl.port: "{{ CALCULATED.dynamicPorts.ssl }}"
                    opentelemetry.exporters.websocket.attach.enabled: false
                    opentelemetry.exporters.websocket.metrics.enabled: true
                    opentelemetry.exporters.websocket.metrics.interval: 2s
                    opentelemetry.exporters.websocket.logging.enabled: false
                    opentelemetry.exporters.logging.enabled: false
                    """)
            .build();

    @Test
    public void testAttachCollector() throws InterruptedException {

        // Given
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

        // When
        Disposable attachDisposable = collector.getServiceLocator().getService(AttachCollectorService.class).attach(exporter.getConfiguration().getURI("opentelemetry.exporters.websocket.listen.uri").orElseThrow());

        // Then
        exporter.getServiceLocator().getService(LoggerContext.class).getLogger("test").info("Test message");
        Span span = exporter.getServiceLocator().getService(OpenTelemetry.class).getTracer(getClass().getName())
                .spanBuilder("test span")
                .setAttribute("someattr", "somevalue")
                .startSpan();
        span.addEvent("someevent");
        span.end();
        Thread.sleep(10000);

        attachDisposable.dispose();

    }
}
