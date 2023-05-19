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
    exports com.exoreaction.xorcery.service.eventstore;
    exports com.exoreaction.xorcery.service.eventstore.api;
    exports com.exoreaction.xorcery.service.eventstore.model;
    exports com.exoreaction.xorcery.service.eventstore.resources.api;
    exports com.exoreaction.xorcery.service.eventstore.streams;
    exports com.exoreaction.xorcery.service.eventstore.projections;

    requires xorcery.reactivestreams.api;
    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi.server;
    requires xorcery.util;

    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires db.client.java;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires com.lmax.disruptor;
    requires jersey.common;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires grpc.shaded.jpms;
}