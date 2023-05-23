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
package com.exoreaction.xorcery.jmxmetrics;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Supplier;

public interface HistogramMXBean
    extends Sampling, Counting
{

    public record Model(Supplier<JsonNode> value) implements HistogramMXBean
    {
        @Override
        public long getCount() {
            return Optional.ofNullable(value.get()).map(ObjectNode.class::cast)
                    .map(o -> o.path("count").longValue())
                    .orElse(0L);
        }

        @Override
        public Snapshot getSnapshot() {
            return new MXSnapshot(value);
        }
    };

    class MXSnapshot extends Snapshot {
        private static final long[] EMPTY_LONG_ARRAY = new long[0];
        private Supplier<JsonNode> value;

        public MXSnapshot(Supplier<JsonNode> value) {

            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getValue(double quantile) {
            return Optional.ofNullable(value.get()).map(ObjectNode.class::cast)
                    .map(o -> o.path("value").doubleValue())
                    .orElse(0D);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long[] getValues() {
            return EMPTY_LONG_ARRAY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getMax() {
            return 0L;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getMean() {
            return 0D;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getMin() {
            return 0L;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getStdDev() {
            return 0D;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dump(OutputStream output) {
            // NOP
        }
    }
}
