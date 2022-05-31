package com.exoreaction.reactiveservices.service.jmxmetrics;

import com.codahale.metrics.Metered;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;
import java.util.function.Supplier;

public interface TimerMXBean
    extends Metered
{

    public record Model(Supplier<JsonNode> value) implements TimerMXBean
    {
        @Override
        public long getCount() {
            return Optional.ofNullable(value.get())
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
            return Optional.ofNullable(value.get())
                    .map(o -> o.path("meanrate").doubleValue())
                    .orElse(0D);
        }

        @Override
        public double getOneMinuteRate() {
            return 0;
        }
    };
}
