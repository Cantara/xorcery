package com.exoreaction.xorcery.jmxmetrics;

import com.exoreaction.xorcery.configuration.ComponentConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;

import java.util.List;

public record JmxConnectorConfiguration(Configuration configuration)
    implements ComponentConfiguration
{
    public String getURI()
    {
        return configuration.getString("uri").orElseThrow(missing("jmxconnector.uri"));
    }

    public String getExternalURI()
    {
        return configuration.getString("externalUri").orElseThrow(missing("jmxconnector.externalUri"));
    }

    public int getRmiPort() {
        return configuration.getInteger("rmiPort").orElse(1100);
    }

    public int getRegistryPort() {
        return configuration.getInteger("registryPort").orElse(1099);
    }

    public boolean isSslEnabled()
    {
        return configuration.getBoolean("ssl.enabled").orElse(false);
    }

    public List<User> getUsers()
    {
        return configuration.getConfigurations("users").stream().map(User::new).toList();
    }

    public record User(Configuration configuration)
    {
        public String getUsername()
        {
            return configuration.getString("username").orElseThrow(Configuration.missing("username"));
        }

        public String getPassword()
        {
            return configuration.getString("password").orElseThrow(Configuration.missing("password"));
        }
    }
}
