package com.exoreaction.reactiveservices.service.jmxmetrics;

public interface ServerMXBean {

    public record Model(String getServerId) implements ServerMXBean{};

    String getServerId();
}
