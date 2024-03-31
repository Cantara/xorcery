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
module xorcery.certificates.client {
    exports com.exoreaction.xorcery.certificates.client;
    exports com.exoreaction.xorcery.certificates.client.resources;

    requires xorcery.certificates.spi;
    requires xorcery.jetty.client;
    requires xorcery.configuration.api;
    requires xorcery.jsonapi.api;
    requires xorcery.dns.client;
    requires org.eclipse.jetty.client;
    requires org.apache.logging.log4j;

    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.util;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires xorcery.keystores;
    requires xorcery.service.api;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.jsonapi.client;
}