package com.exoreaction.reactiveservices.service.jmxmetrics;

import com.codahale.metrics.Gauge;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;

import java.util.Optional;
import java.util.function.Supplier;

public interface GaugeMXBean
    extends Gauge<Long>
{
    public record Model(Supplier<JsonValue> value) implements GaugeMXBean
    {
        @Override
        public Long getValue() {
            return Optional.ofNullable((JsonNumber) value.get()).orElse(Json.createValue(0L)).longValue();
        }
    };

    @Override
    Long getValue();
}
