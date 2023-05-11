package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.util.Optional;

public record ClientConfiguration(Configuration configuration) {

    public static ClientConfiguration defaults()
    {
        return new ClientConfiguration(new Configuration.Builder().build());
    }

    public record Builder(Configuration.Builder builder) {
        public Builder() {
            this(new Configuration.Builder());
        }

        public Builder isRetryEnabled(boolean value) {
            builder.add("retry.enabled", value);
            return this;
        }

        public Builder retryDelay(String duration) {
            builder.add("retry.duration", duration);
            return this;
        }

        public Builder scheme(String wsOrWss) {
            builder.add("scheme", wsOrWss);
            return this;
        }

        public Builder disruptorSize(int value) {
            builder.add("disruptor.size", value);
            return this;
        }

        public ClientConfiguration build() {
            return new ClientConfiguration(builder.build());
        }
    }

    public boolean isRetryEnabled() {
        return configuration.getBoolean("retry.enabled").orElse(true);
    }

    public String getRetryDelay() {
        return configuration.getString("retry.delay").orElse("10S");
    }

    public Optional<String> getScheme() {
        return configuration.getString("scheme");
    }

    public int getDisruptorSize() {
        return configuration.getInteger("disruptor.size").orElse(512);
    }
}
