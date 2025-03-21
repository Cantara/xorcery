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
module xorcery.opentelemetry.sdk {

    exports dev.xorcery.opentelemetry.sdk;
    exports dev.xorcery.opentelemetry.exporters.jmx;
    exports dev.xorcery.opentelemetry.exporters.local;
    exports dev.xorcery.opentelemetry.exporters.logging;
    exports dev.xorcery.opentelemetry.exporters.otlphttp;
    exports dev.xorcery.opentelemetry.exporters.otlphttp.jdk;

    requires xorcery.configuration.api;
    requires xorcery.secrets.api;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires org.apache.logging.log4j;

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
    requires java.management;
    requires io.opentelemetry.semconv.incubating;

    provides io.opentelemetry.exporter.internal.http.HttpSenderProvider with dev.xorcery.opentelemetry.exporters.otlphttp.jdk.JdkHttpSenderProvider;
}