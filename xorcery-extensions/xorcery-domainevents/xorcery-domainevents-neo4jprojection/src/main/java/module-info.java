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

import dev.xorcery.neo4j.spi.Neo4jProvider;

module xorcery.domainevents.neo4jprojection {
    exports dev.xorcery.domainevents.neo4jprojection;
    exports dev.xorcery.domainevents.neo4jprojection.providers;

    requires xorcery.neo4j.embedded;
    requires xorcery.domainevents.api;
    requires xorcery.domainevents.entity;
    requires xorcery.neo4j.shaded;
    requires xorcery.neo4j.projections;
    requires xorcery.domainevents.publisher;
    requires xorcery.reactivestreams.api;

    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires jakarta.ws.rs;
    requires jakarta.inject;

    provides Neo4jProvider with dev.xorcery.domainevents.neo4jprojection.providers.ApplyJsonDomainEvent;
}