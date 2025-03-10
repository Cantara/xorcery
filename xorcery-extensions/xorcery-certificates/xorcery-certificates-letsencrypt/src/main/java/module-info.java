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
module xorcery.certificates.letsencrypt {
    exports dev.xorcery.certificates.letsencrypt;
    exports dev.xorcery.certificates.letsencrypt.resources;

    requires xorcery.certificates.spi;
    requires xorcery.keystores;
    requires xorcery.secrets.api;
    requires xorcery.secrets.spi;
    requires xorcery.util;
    requires xorcery.configuration.api;

    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.bouncycastle.provider;
    requires org.shredzone.acme4j;
    requires org.apache.logging.log4j;
    requires org.shredzone.acme4j.utils;
    requires info.picocli;
    requires org.bouncycastle.pkix;
    requires java.base;
}