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
module xorcery.reactivestreams.server {
    exports dev.xorcery.reactivestreams.server;

    requires transitive xorcery.reactivestreams.api;

    requires xorcery.jetty.server;
    requires static xorcery.status.spi;
    requires xorcery.opentelemetry.api;

    requires reactor.core;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.eclipse.jetty.websocket.server;
    requires org.eclipse.jetty.ee10.websocket.jetty.server;
    requires org.eclipse.jetty.server;

    requires org.apache.logging.log4j;

    requires io.opentelemetry.api;
    requires io.opentelemetry.semconv;
    requires io.opentelemetry.semconv.incubating;
    requires io.opentelemetry.context;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
}