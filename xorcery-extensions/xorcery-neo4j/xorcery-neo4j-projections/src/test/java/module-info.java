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
open module xorcery.neo4j.projections.test {
    requires xorcery.neo4j.projections;
    requires org.junit.jupiter.api;
    requires xorcery.junit;
    requires xorcery.domainevents.api;
    requires xorcery.reactivestreams.extras;
    requires reactor.core;
    requires xorcery.neo4j.embedded;
    requires xorcery.neo4j.shaded;
    requires xorcery.reactivestreams.api;
    requires xorcery.reactivestreams.server;
    requires jakarta.ws.rs;
}