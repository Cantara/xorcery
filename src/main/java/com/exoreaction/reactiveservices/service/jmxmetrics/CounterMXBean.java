package com.exoreaction.reactiveservices.service.jmxmetrics;

import com.codahale.metrics.Counting;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;

import java.util.Optional;
import java.util.function.Supplier;

public interface CounterMXBean
        extends Counting {
    public record Model(Supplier<JsonNode> value) implements CounterMXBean {
        @Override
        public long getCount() {
            return Optional.ofNullable((NumericNode) value.get()).orElse(JsonNodeFactory.instance.numberNode(0L)).longValue();
        }
    }

    ;

    @Override
    long getCount();
}
