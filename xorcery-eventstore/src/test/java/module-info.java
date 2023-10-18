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
open module xorcery.eventstore.test {
    requires xorcery.junit;
    requires xorcery.eventstore;
    requires xorcery.reactivestreams.server;
    requires xorcery.jetty.client;
    requires org.junit.jupiter.api;
    requires testcontainers;
    requires org.slf4j;
    requires db.client.java;
    requires junit.jupiter;
    requires junit;
    requires jakarta.activation;
    requires jakarta.annotation;
    requires org.glassfish.hk2.api;
    requires org.apache.commons.compress;
}
