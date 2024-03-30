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
open module xorcery.eventstore {
    exports com.exoreaction.xorcery.eventstore;
    exports com.exoreaction.xorcery.eventstore.api;
    exports com.exoreaction.xorcery.eventstore.streams;
    exports com.exoreaction.xorcery.eventstore.projections;
    exports com.exoreaction.xorcery.eventstore.reactor;
    exports com.exoreaction.xorcery.eventstore.resources;

    requires xorcery.reactivestreams.api;
    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi.server;
    requires xorcery.jaxrs.server;
    requires xorcery.util;
    requires xorcery.domainevents;
    requires xorcery.opentelemetry.api;

    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires db.client.java;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires com.lmax.disruptor;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires io.opentelemetry.semconv;
    requires grpc.shaded.jpms;
}