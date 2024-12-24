/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
