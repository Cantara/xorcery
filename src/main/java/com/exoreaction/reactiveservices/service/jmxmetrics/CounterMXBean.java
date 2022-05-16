package com.exoreaction.reactiveservices.service.jmxmetrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;

import java.util.Optional;
import java.util.function.Supplier;

public interface CounterMXBean
    extends Counting
{
    public record Model(Supplier<JsonValue> value) implements CounterMXBean
    {
        @Override
        public long getCount() {
            return Optional.ofNullable((JsonNumber) value.get()).orElse(Json.createValue(0L)).longValue();
        }
    };
    @Override
    long getCount();
}
