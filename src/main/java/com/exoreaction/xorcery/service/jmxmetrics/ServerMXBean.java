package com.exoreaction.xorcery.service.jmxmetrics;

public interface ServerMXBean {

    public record Model(String getServerId) implements ServerMXBean{};

    String getServerId();
}
