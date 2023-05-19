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
open module xorcery.handlebars {
    exports com.exoreaction.xorcery.service.handlebars.jaxrs.providers;
    exports com.exoreaction.xorcery.service.handlebars.helpers;
    exports com.exoreaction.xorcery.service.handlebars;
    exports com.exoreaction.xorcery.service.handlebars.resources.api;

    requires xorcery.configuration.api;
    requires xorcery.jsonschema;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.jaxrs;

    requires org.glassfish.hk2.api;
    requires handlebars;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires java.logging;
    requires com.fasterxml.jackson.databind;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires jersey.common;
}