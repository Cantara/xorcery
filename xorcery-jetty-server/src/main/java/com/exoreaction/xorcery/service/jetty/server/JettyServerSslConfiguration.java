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
package com.exoreaction.xorcery.service.jetty.server;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.net.URL;
import java.util.Optional;

public record JettyServerSslConfiguration(Configuration configuration) {

    public boolean isEnabled() {
        return configuration.getBoolean("enabled").orElse(false);
    }

    public Optional<URL> getCRLs() {
        return configuration.getResourceURL("crls");
    }

    public String getAlias() {
        return configuration.getString("alias").orElse("self");
    }

    public int getPort() {
        return configuration.getInteger("port").orElse(443);
    }

    public boolean isTrustAll() {
        return configuration.getBoolean("trustAll").orElse(false);
    }

    public boolean isNeedClientAuth() {
        return configuration.getBoolean("needClientAuth").orElse(false);
    }

    public boolean isWantClientAuth() {
        return configuration.getBoolean("wantClientAuth").orElse(false);
    }

    public boolean isSniRequired() {
        return configuration.getBoolean("sniRequired").orElse(false);
    }

    public boolean isSniHostCheck() {
        return configuration.getBoolean("ssl.sniHostCheck").orElse(false);
    }
}
