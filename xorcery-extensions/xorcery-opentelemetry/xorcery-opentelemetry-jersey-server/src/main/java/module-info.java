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
/**
 * @author rickardoberg
 * @since 18/01/2024
 */

module xorcery.opentelemetry.jersey.server {
    exports com.exoreaction.xorcery.opentelemetry.jersey.server.resources;

    requires io.opentelemetry.api;
    requires io.opentelemetry.context;
    requires io.opentelemetry.semconv;
    requires xorcery.configuration.api;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires org.eclipse.jetty.server;
    requires jersey.server;
    requires jersey.common;
    requires xorcery.opentelemetry.api;
    requires org.apache.logging.log4j;

}