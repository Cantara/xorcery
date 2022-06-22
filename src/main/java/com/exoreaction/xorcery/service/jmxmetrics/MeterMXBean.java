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
