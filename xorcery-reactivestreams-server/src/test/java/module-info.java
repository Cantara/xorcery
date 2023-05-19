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
open module xorcery.reactivestreams.server.test {
    requires xorcery.reactivestreams.client;
    requires xorcery.reactivestreams.server;

    requires xorcery.core;
    requires xorcery.core.test;
    requires xorcery.jetty.client;
    requires xorcery.jetty.server;

    requires jakarta.ws.rs;
    requires org.glassfish.hk2.api;
    requires com.fasterxml.jackson.datatype.jsr310;
}