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
open module xorcery.log4j {
    exports com.exoreaction.xorcery.service.log4jpublisher.log4j;
    exports com.exoreaction.xorcery.service.log4jpublisher;
    exports com.exoreaction.xorcery.service.log4jsubscriber;
    exports com.exoreaction.xorcery.service.requestlogpublisher;
    exports com.exoreaction.xorcery.service.log4jpublisher.providers;

    requires xorcery.util;
    requires xorcery.configuration.api;
    requires xorcery.metadata;
    requires xorcery.disruptor;
    requires xorcery.reactivestreams.api;

    requires com.lmax.disruptor;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires jakarta.inject;
    requires com.fasterxml.jackson.databind;
    requires log4j.layout.template.json;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires org.glassfish.hk2.utilities;
    requires org.eclipse.jetty.server;
}