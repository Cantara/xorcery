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
package com.exoreaction.xorcery.service.jmxmetrics;

import com.codahale.metrics.Metered;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;
import java.util.function.Supplier;

public interface MeterMXBean
    extends Metered
{

    public record Model(Supplier<JsonNode> value) implements MeterMXBean
    {
        @Override
        public long getCount() {
            return Optional.ofNullable(value.get()).map(ObjectNode.class::cast)
                    .map(o -> o.path("count").longValue())
                    .orElse(0L);
        }

        @Override
        public double getFifteenMinuteRate() {
            return 0;
        }

        @Override
        public double getFiveMinuteRate() {
            return 0;
        }

        @Override
        public double getMeanRate() {
            return Optional.ofNullable(value.get()).map(ObjectNode.class::cast)
                    .map(o -> o.path("meanrate").doubleValue())
                    .orElse(0D);
        }

        @Override
        public double getOneMinuteRate() {
            return 0;
        }
    };
}
