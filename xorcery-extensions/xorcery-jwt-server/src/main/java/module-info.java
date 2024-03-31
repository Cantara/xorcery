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
module xorcery.jwt.server {

    requires transitive xorcery.jsonapi.server;
    requires transitive xorcery.metadata;

    requires xorcery.configuration.api;

    requires org.eclipse.jetty.security;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.annotation;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.ws.rs;
    requires xorcery.domainevents.jsonapi;
    requires xorcery.jetty.server;
    requires com.auth0.jwt;
    requires xorcery.service.api;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires xorcery.secrets.api;
    requires xorcery.jaxrs.server;
    requires jakarta.servlet;

    requires org.eclipse.jetty.ee10.servlet;
}