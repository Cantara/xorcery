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
package dev.xorcery.jetty.client;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ServiceConfiguration;

import java.time.Duration;

public record JettyClientConfiguration(Configuration context)
        implements ServiceConfiguration {
    public Duration getIdleTimeout() {
        return Duration.parse("PT" + context().getString("idleTimeout").orElse("-1s"));
    }

    public Duration getConnectTimeout() {
        return Duration.parse("PT" + context().getString("connectTimeout").orElse("5s"));
    }

    public boolean getReusePort() {
        return context().getBoolean("reusePort").orElse(false);
    }

    public int getRequestBufferSize() {
        return context().getInteger("requestBufferSize").orElse(4096);
    }

    public JettyHttp2Configuration getHTTP2Configuration() {
        return new JettyHttp2Configuration(context.getConfiguration("http2"));
    }

    public JettyClientSslConfiguration getSSLConfiguration() {
        return new JettyClientSslConfiguration(context.getConfiguration("ssl"));
    }
}
