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
module xorcery.reactivestreams.api {
    exports com.exoreaction.xorcery.reactivestreams.api;
    exports com.exoreaction.xorcery.reactivestreams.api.client;
    exports com.exoreaction.xorcery.reactivestreams.api.server;
    exports com.exoreaction.xorcery.reactivestreams.spi;
    exports com.exoreaction.xorcery.reactivestreams.providers;
    exports com.exoreaction.xorcery.reactivestreams.util;

    opens com.exoreaction.xorcery.reactivestreams.api;

    requires transitive reactor.core;
    requires transitive org.reactivestreams;
    requires transitive xorcery.metadata;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}