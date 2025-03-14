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
module xorcery.thymeleaf.jsonapi {
    exports dev.xorcery.thymeleaf.jsonapi.resources;
    exports dev.xorcery.thymeleaf.jsonapi.providers;

    requires xorcery.configuration.api;
    requires xorcery.jsonapi.server;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.jaxrs;

    requires thymeleaf;
    requires jakarta.servlet;
    requires org.eclipse.jetty.ee10.servlet;
    requires jakarta.ws.rs;
    requires jersey.server;
    requires jersey.common;

    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;

    opens templates.jsonapi;
    opens templates.jsonapi.fragment;
}