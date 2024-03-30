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
open module xorcery.domainevents.publisher {
    exports com.exoreaction.xorcery.domainevents.helpers.entity;
    exports com.exoreaction.xorcery.domainevents.helpers.entity.annotation;
    exports com.exoreaction.xorcery.domainevents.helpers.context;
    exports com.exoreaction.xorcery.domainevents.helpers.model;
    exports com.exoreaction.xorcery.domainevents.publisher;

    requires transitive xorcery.jsonapi.server;
    requires transitive xorcery.metadata;

    requires xorcery.reactivestreams.api;
    requires xorcery.configuration.api;

    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.annotation;
    requires jakarta.inject;
    requires jakarta.validation;
    requires org.glassfish.hk2.api;
    requires xorcery.domainevents.api;
    requires jakarta.ws.rs;
}