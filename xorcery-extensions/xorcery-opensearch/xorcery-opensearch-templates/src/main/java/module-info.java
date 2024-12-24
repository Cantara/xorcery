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
module xorcery.opensearch.templates {
    exports dev.xorcery.opensearch.templates;
    opens opensearch.templates;
    opens opensearch.templates.components;

    requires xorcery.opensearch.client;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires io.opentelemetry.api;
    requires xorcery.reactivestreams.api;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}