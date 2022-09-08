package com.exoreaction.xorcery.service.jmxmetrics;

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
