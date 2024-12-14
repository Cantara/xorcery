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
module xorcery.jetty.server {
    exports dev.xorcery.jetty.server;
    exports dev.xorcery.jetty.server.providers;
    exports dev.xorcery.jetty.server.security;
    exports dev.xorcery.jetty.server.security.jwt;
    exports dev.xorcery.jetty.server.security.clientcert;

    requires transitive xorcery.configuration.api;
    requires xorcery.secrets.api;
    requires xorcery.keystores;
    requires xorcery.util;

    requires jakarta.inject;
    requires jakarta.validation;
    requires jakarta.annotation;
    requires jakarta.activation;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    // Jetty
    requires transitive org.eclipse.jetty.server;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.eclipse.jetty.util;
    requires org.eclipse.jetty.alpn.java.server;

    // Websockets
    requires org.eclipse.jetty.websocket.server;
    requires org.eclipse.jetty.ee10.websocket.jetty.server;

    // JWT Authenticator
    requires com.auth0.jwt;
    requires org.bouncycastle.provider;

    requires org.apache.logging.log4j;

}