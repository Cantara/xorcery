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

open module xorcery.neo4j.projections {
    uses dev.xorcery.neo4jprojections.spi.Neo4jEventProjection;

    exports dev.xorcery.neo4jprojections;
    exports dev.xorcery.neo4jprojections.api;
    exports dev.xorcery.neo4jprojections.providers;
    exports dev.xorcery.neo4jprojections.spi;

    requires xorcery.domainevents.api;
    requires xorcery.neo4j.embedded;
    requires xorcery.neo4j.shaded;
    requires xorcery.reactivestreams.api;
    requires xorcery.reactivestreams.extras;
    requires xorcery.opentelemetry.api;

    requires reactor.core;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires io.opentelemetry.api;
    requires io.opentelemetry.semconv;
    requires io.opentelemetry.context;

    provides dev.xorcery.neo4jprojections.spi.Neo4jEventProjection with dev.xorcery.neo4jprojections.providers.CypherEventProjection;
}