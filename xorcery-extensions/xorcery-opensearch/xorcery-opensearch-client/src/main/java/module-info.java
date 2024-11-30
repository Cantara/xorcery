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
module xorcery.opensearch.client {
    exports dev.xorcery.opensearch.client.document;
    exports dev.xorcery.opensearch.client.index;
    exports dev.xorcery.opensearch.client.jaxrs;
    exports dev.xorcery.opensearch.client.search;
    exports dev.xorcery.opensearch.client;
    exports dev.xorcery.opensearch;

    opens opensearch.templates.components;
    opens opensearch.templates;

    requires xorcery.reactivestreams.extras;
    requires xorcery.reactivestreams.api;
    requires xorcery.jersey.client;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.util;

    requires io.opentelemetry.api;
    requires org.apache.logging.log4j;
    requires io.opentelemetry.context;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.ws.rs;
}