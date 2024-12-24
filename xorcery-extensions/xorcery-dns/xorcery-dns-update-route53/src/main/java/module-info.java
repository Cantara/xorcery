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
module xorcery.dns.update.routefiftythree {
    exports dev.xorcery.dns.update.route53;

    requires xorcery.configuration.api;
    requires xorcery.dns.update;

    requires org.dnsjava;
    requires software.amazon.awssdk.services.route53;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.auth;
    requires xorcery.secrets.api;
    requires software.amazon.awssdk.awscore;
    requires software.amazon.awssdk.core;
}
