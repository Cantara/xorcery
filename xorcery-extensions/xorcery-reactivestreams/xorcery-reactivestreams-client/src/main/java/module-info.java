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
module xorcery.reactivestreams.client {
    exports dev.xorcery.reactivestreams.client;

    requires transitive xorcery.reactivestreams.api;
    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.dns.client;
    requires xorcery.opentelemetry.api;

    requires org.reactivestreams;
    requires reactor.core;
    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.io;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires io.opentelemetry.api;
    requires io.opentelemetry.semconv;
    requires io.opentelemetry.semconv.incubating;
    requires io.opentelemetry.context;
    requires org.eclipse.jetty.client;
    requires org.eclipse.jetty.websocket.api;
    requires org.eclipse.jetty.websocket.client;
    requires org.eclipse.jetty.websocket.common;
}