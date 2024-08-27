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
module xorcery.reactivestreams.extras {
    exports com.exoreaction.xorcery.reactivestreams.extras.operators;
    exports com.exoreaction.xorcery.reactivestreams.extras.publishers;
    exports com.exoreaction.xorcery.reactivestreams.extras.providers;
    exports com.exoreaction.xorcery.reactivestreams.extras.subscribers;
    exports com.exoreaction.xorcery.reactivestreams.extras.loadbalancing;

    requires xorcery.reactivestreams.api;
    requires xorcery.util;
    requires xorcery.dns.client;

    requires reactor.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.dataformat.smile;
    requires org.reactivestreams;
    requires org.yaml.snakeyaml;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.eclipse.jetty.client;
    requires org.apache.logging.log4j;
}