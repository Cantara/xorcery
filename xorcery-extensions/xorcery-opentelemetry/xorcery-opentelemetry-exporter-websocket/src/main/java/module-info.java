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