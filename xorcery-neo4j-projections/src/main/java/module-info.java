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
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.neo4jprojections.streams.CypherEventProjection;

open module xorcery.neo4j.projections {
    uses Neo4jEventProjection;

    exports com.exoreaction.xorcery.neo4jprojections.api;
    exports com.exoreaction.xorcery.neo4jprojections.spi;

    requires xorcery.service.api;
    requires xorcery.neo4j;
    requires xorcery.neo4j.shaded;
    requires xorcery.jsonapi.server;
    requires xorcery.reactivestreams.api;
    requires xorcery.disruptor;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires io.opentelemetry.api;
    requires io.opentelemetry.semconv;
    requires xorcery.domainevents;

    provides Neo4jEventProjection with CypherEventProjection;
}