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
open module xorcery.jetty.client {
    exports com.exoreaction.xorcery.jetty.client;
    exports com.exoreaction.xorcery.jetty.client.providers;

    requires xorcery.configuration.api;
    requires xorcery.secrets;
    requires xorcery.keystores;
    requires xorcery.dns.client;

    requires transitive org.eclipse.jetty.client;

    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.http2.client;
    requires org.eclipse.jetty.http2.client.transport;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    requires io.opentelemetry.semconv;
}