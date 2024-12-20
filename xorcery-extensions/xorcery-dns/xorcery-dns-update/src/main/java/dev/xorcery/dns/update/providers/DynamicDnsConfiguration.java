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
package dev.xorcery.dns.update.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ServiceConfiguration;

import java.time.Duration;
import java.util.Optional;

public record DynamicDnsConfiguration(Configuration context)
        implements ServiceConfiguration {
    public Duration getTimeout() {
        return Duration.parse("PT" + context().getString("timeout").orElse("30s"));
    }

    public Duration getTtl() {
        return Duration.parse("PT" + context().getString("ttl").orElse("60s"));
    }

    public Optional<DnsKeyConfiguration> getKey() {
        return context.getObjectAs("key", Configuration::new).map(DnsKeyConfiguration::new);
    }
}
