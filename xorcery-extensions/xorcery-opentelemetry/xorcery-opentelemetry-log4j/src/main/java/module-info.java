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
 * @since 19/01/2024
 */

module xorcery.opentelemetry.log4j {
    exports dev.xorcery.opentelemetry.log4j;

    requires xorcery.opentelemetry.api;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires io.opentelemetry.instrumentation.log4j_appender_2_17;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires io.opentelemetry.sdk.trace;

    provides org.apache.logging.log4j.core.util.ContextDataProvider with dev.xorcery.opentelemetry.log4j.SpanContextDataProvider;
}