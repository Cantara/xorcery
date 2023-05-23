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
open module xorcery.reactivestreams.client {
    exports com.exoreaction.xorcery.reactivestreams.client;
    exports com.exoreaction.xorcery.reactivestreams.common;

    requires transitive xorcery.reactivestreams.api;
    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.dns.client;

    requires com.lmax.disruptor;
    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.io;
    requires org.eclipse.jetty.websocket.jetty.api;
    requires org.eclipse.jetty.websocket.jetty.client;
    requires com.codahale.metrics;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
}