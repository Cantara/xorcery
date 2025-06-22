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
open module xorcery.kurrent.client.test {
    requires xorcery.kurrent.client;
    requires xorcery.reactivestreams.api;
    requires xorcery.configuration;
    requires xorcery.junit;
    requires xorcery.metadata;
    requires xorcery.reactivestreams.extras;
    requires kurrentdb.client;
    requires org.reactivestreams;

    requires org.slf4j;
    requires testcontainers;
    requires junit.jupiter;
    requires reactor.core;
    requires xorcery.log4j;
    requires junit;
    requires org.apache.commons.compress;
}