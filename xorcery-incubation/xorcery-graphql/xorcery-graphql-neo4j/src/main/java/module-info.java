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
module xorcery.graphql.neo4j {
    exports dev.xorcery.graphql.server.jsonapi.api;
    exports dev.xorcery.graphql.server.neo4j.cypher;
    exports dev.xorcery.graphql.server.schema;
    exports dev.xorcery.graphql;

    requires xorcery.jsonapi.server;
    requires xorcery.jaxrs.server;
    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires xorcery.configuration.api;
    requires org.glassfish.hk2.api;
    requires com.graphqljava;
}