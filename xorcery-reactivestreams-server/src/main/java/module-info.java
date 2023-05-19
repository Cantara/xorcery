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
open module xorcery.reactivestreams.server {
    exports com.exoreaction.xorcery.service.reactivestreams.server;

    requires transitive xorcery.reactivestreams.api;

    requires xorcery.reactivestreams.client;
    requires xorcery.dns.client;

    requires xorcery.service.api;
    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.jetty.server;
    requires xorcery.jetty.client;

    requires org.eclipse.jetty.websocket.jetty.server;
    requires com.lmax.disruptor;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.server;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires com.codahale.metrics;
}