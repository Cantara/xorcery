/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.configuration;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * Configuration wrapper for instance configuration
 *
 * @author rickardoberg
 * @since 20/04/2022
 */
public record InstanceConfiguration(Configuration configuration) {
    public static InstanceConfiguration get(Configuration configuration)
    {
        return new InstanceConfiguration(configuration.getConfiguration("instance"));
    }

    public String getId() {
        return configuration.getString("id").orElse(null);
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

    public List<String> getTags() {
        return configuration.getListAs("tags", JsonNode::textValue).orElse(Collections.emptyList());
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
