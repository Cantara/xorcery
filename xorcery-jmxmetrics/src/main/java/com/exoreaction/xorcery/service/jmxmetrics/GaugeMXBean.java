package com.exoreaction.xorcery.service.jmxmetrics;

import com.codahale.metrics.Gauge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;

import java.util.Optional;
import java.util.function.Supplier;

public interface GaugeMXBean
        extends Gauge<Long> {
    public record Model(Supplier<JsonNode> value) implements GaugeMXBean {
        @Override
        public Long getValue() {
            return Optional.ofNullable((NumericNode) value.get()).orElse(JsonNodeFactory.instance.numberNode(0L)).longValue();
        }
    }
    @Override
    Long getValue();
}
