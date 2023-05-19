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
open module xorcery.jetty.server {
    exports com.exoreaction.xorcery.service.jetty.server;
    exports com.exoreaction.xorcery.service.jetty.server.security;
    exports com.exoreaction.xorcery.service.jetty.server.security.jwt;
    exports com.exoreaction.xorcery.service.jetty.server.security.clientcert;

    requires transitive xorcery.configuration.api;
    requires transitive xorcery.metricregistry;
    requires xorcery.keystores;
    requires xorcery.health.api;
    requires xorcery.health.registry;

    requires jakarta.inject;
    requires jakarta.validation;
    requires jakarta.annotation;
    requires jakarta.activation;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    requires io.dropwizard.metrics.jetty11;

    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.alpn.java.server;
    requires org.eclipse.jetty.http2.server;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.websocket.jetty.server;

    requires org.apache.logging.log4j;
    requires com.codahale.metrics;

    requires jjwt.api;
}