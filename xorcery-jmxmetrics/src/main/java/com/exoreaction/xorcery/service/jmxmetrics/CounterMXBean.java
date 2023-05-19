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

import com.codahale.metrics.Counting;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;

import java.util.Optional;
import java.util.function.Supplier;

public interface CounterMXBean
        extends Counting {
    record Model(Supplier<JsonNode> value) implements CounterMXBean {
        @Override
        public long getCount() {
            return Optional.ofNullable((NumericNode) value.get()).orElse(JsonNodeFactory.instance.numberNode(0L)).longValue();
        }
    }

    @Override
    long getCount();
}
