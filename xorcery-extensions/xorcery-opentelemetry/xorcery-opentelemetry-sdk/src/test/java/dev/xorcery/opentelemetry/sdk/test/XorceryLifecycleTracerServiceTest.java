package dev.xorcery.opentelemetry.sdk.test;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.opentelemetry.exporters.local.LocalSpanExporter;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.Test;

import java.util.List;

public class XorceryLifecycleTracerServiceTest {

    @Test
    public void testStartupShutdown() throws Exception {

        LocalSpanExporter localSpanExporter;
        try (Xorcery xorcery = new Xorcery(new ConfigurationBuilder()
                .addTestDefaults()
                .addYaml("""
                defaults.development: true
                """)
                .build()))
        {
            // Started
            localSpanExporter = xorcery.getServiceLocator().getService(LocalSpanExporter.class);
        }
        List<SpanData> serverSpans = localSpanExporter.getSpans("server");
        for (SpanData serverSpan : serverSpans) {
            System.out.println(serverSpan.getName());
            for (EventData event : serverSpan.getEvents()) {
                System.out.println("  "+event.getName());
            }
        }
    }

    @Test
    public void testStartupFailure() throws Exception {

        LocalSpanExporter localSpanExporter;
        try (Xorcery xorcery = new Xorcery(new ConfigurationBuilder()
                .addTestDefaults()
                .addYaml("""
                defaults.development: true
                failing.enabled: true
                """)
                .build()))
        {
            // Fails
        } catch (Throwable throwable)
        {
            // Ignore
        }
        List<SpanData> serverSpans = FailingService.localSpanExporter.getSpans("server");
        for (SpanData serverSpan : serverSpans) {
            System.out.println(serverSpan.getName());
            for (EventData event : serverSpan.getEvents()) {
                System.out.println("  "+event.getName());
            }
        }
    }
}
