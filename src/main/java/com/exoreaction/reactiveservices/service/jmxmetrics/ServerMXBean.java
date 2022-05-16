package com.exoreaction.reactiveservices.service.jmxmetrics;

import javax.management.MXBean;

public interface ServerMXBean {

    public record Model(String getServerId) implements ServerMXBean{};

    String getServerId();
}
