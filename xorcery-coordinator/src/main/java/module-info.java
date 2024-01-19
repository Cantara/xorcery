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
open module xorcery.coordinator {
    exports com.exoreaction.xorcery.coordinator;

    requires info.picocli;
    requires transitive xorcery.runner;
    requires transitive xorcery.log4j;
    requires transitive xorcery.reactivestreams.server;
    requires transitive xorcery.jersey.server;
    requires transitive xorcery.jersey.client;
    requires transitive xorcery.handlebars;
    requires transitive xorcery.dns.server;
    requires transitive xorcery.dns.registration;
    requires transitive xorcery.certificates.server;
    requires transitive xorcery.certificates.ca;
    requires transitive xorcery.certificates.letsencrypt;
    requires transitive xorcery.jwt.server;
    requires transitive xorcery.status.server;

    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j.layout.template.json;
    requires java.net.http;
}