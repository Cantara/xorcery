package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.health.api.HealthCheckAppInfo;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@RunLevel
@ContractsProvided({HealthCheckAppInfo.class, DefaultHealthCheckAppInfo.class})
public class DefaultHealthCheckAppInfo implements HealthCheckAppInfo {

    private String version;
    private String name;

    DefaultHealthCheckAppInfo() {
    }

    public synchronized DefaultHealthCheckAppInfo version(String version) {
        this.version = version;
        return this;
    }

    public synchronized DefaultHealthCheckAppInfo name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public synchronized String version() {
        return version;
    }

    @Override
    public synchronized String name() {
        return name;
    }
}
