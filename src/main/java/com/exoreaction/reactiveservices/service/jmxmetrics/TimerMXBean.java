package com.exoreaction.reactiveservices.service.jmxmetrics;

import com.codahale.metrics.Metered;
import jakarta.json.JsonValue;

import java.util.Optional;
import java.util.function.Supplier;

public interface TimerMXBean
    extends Metered
{

    public record Model(Supplier<JsonValue> value) implements TimerMXBean
    {
        @Override
        public long getCount() {
            return Optional.ofNullable(value.get()).map(JsonValue::asJsonObject)
                    .map(o -> o.getJsonNumber("count").longValue())
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
            return Optional.ofNullable(value.get()).map(JsonValue::asJsonObject)
                    .map(o -> o.getJsonNumber("meanrate").doubleValue())
                    .orElse(0D);
        }

        @Override
        public double getOneMinuteRate() {
            return 0;
        }
    };
}
