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
open module xorcery.benchmarks {
    exports com.exoreaction.xorcery.benchmarks.reactivestreams;
    exports com.exoreaction.xorcery.benchmarks.reactivestreams.jmh_generated;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi.client;
    requires xorcery.keystores;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.configuration;
    requires xorcery.core;
    requires xorcery.metadata;
    requires xorcery.reactivestreams.api;
    requires jmh.core;
    requires xorcery.jsonapi.server;
    requires jdk.unsupported;
    requires xorcery.reactivestreams.server;
}