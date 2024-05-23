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
package com.exoreaction.xorcery.opentelemetry.sdk.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

class OpenTelemetrySDKFactoryTest {

    static String config = """
            opentelemetry.exporters.otlp.endpoint: "https://otlp.eu01.nr-data.net"
            opentelemetry.exporters.otlp.http.headers:
                api-key: env:apikey
            """;

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
//            .addYaml(config)
            .build();

    @Test
    public void test(OpenTelemetry openTelemetry) throws InterruptedException {

        // Logging
        for (int i = 0; i < 100; i++) {
            LogManager.getLogger().info("TEST "+i);
        }

        // Metrics
        Meter someMetric = openTelemetry.meterBuilder("somemetric").build();
        LongCounter counter = someMetric.counterBuilder("count").build();

        counter.add(3);

        // Tracing
        Tracer tracer = openTelemetry.getTracer(getClass().getName());

        Span span = tracer.spanBuilder("foo").setAttribute("x", "y").setStartTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS).startSpan();
        span.addEvent("foobar");
        span.addEvent("fooevent", Attributes.builder().put("x", "a").build());
        span.addEvent("fooevent", Attributes.builder().put("x", "b").build());
        span.addEvent("fooevent", Attributes.builder().put("x", "c").build());
        span.end();

        System.out.println("DONE");
//        Thread.sleep(10000);
    }
}