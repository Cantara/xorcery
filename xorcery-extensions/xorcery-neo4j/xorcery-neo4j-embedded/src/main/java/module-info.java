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

module xorcery.neo4j.embedded {
    uses dev.xorcery.neo4j.spi.Neo4jProvider;

    exports dev.xorcery.neo4j.client;
    exports dev.xorcery.neo4j;
    exports dev.xorcery.neo4j.providers;
    exports dev.xorcery.neo4j.spi;

    requires xorcery.configuration.api;

    requires jdk.unsupported;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires io.opentelemetry.api;
    requires io.opentelemetry.semconv;
    requires org.glassfish.hk2.runlevel;
    requires org.apache.commons.lang3;
    requires com.sun.jna;
    requires xorcery.opentelemetry.api;
    requires com.fasterxml.jackson.databind;
    requires xorcery.neo4j.shaded;

    provides dev.xorcery.neo4j.spi.Neo4jProvider with dev.xorcery.neo4j.providers.TransactionContextExtensions;
}