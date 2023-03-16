package com.exoreaction.xorcery.configuration.model;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Configuration wrapper for instance configuration
 *
 * @author rickardoberg
 * @since 20/04/2022
 */
public record InstanceConfiguration(Configuration configuration) {
    public String getId() {
        return configuration.getString("id").orElse(null);
    }

    public String getName() {
        return configuration.getString("name").orElse(null);
    }

    public String getHost() {
        return configuration.getString("host").orElse(null);
    }

    public String getFQDN() {
        return configuration.getString("fqdn").orElse(null);
    }

    public InetAddress getIp() {
        return configuration.getString("ip").map(ip ->
        {
            try {
                return InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                throw new UncheckedIOException(e);
            }
        }).orElseGet(() ->
        {
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public String getDomain() {
        return configuration.getString("domain").orElse(null);
    }

    public String getEnvironment() {
        return configuration.getString("environment").orElse(null);
    }

    public String getTag() {
        return configuration.getString("tag").orElse(null);
    }

    public String getHome() {
        return configuration.getString("home").orElseGet(() ->
        {
            try {
                return new File(".").getCanonicalPath();
            } catch (IOException e) {
                return new File(".").getAbsolutePath();
            }
        });
    }

    public String getResources() {
        return configuration.getString("resources").orElse(null);
    }

    public URI getURI() {
        return configuration.getURI("uri").orElseThrow();
    }
}
