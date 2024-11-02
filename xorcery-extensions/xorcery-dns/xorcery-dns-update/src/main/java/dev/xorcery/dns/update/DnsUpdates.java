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
package dev.xorcery.dns.update;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.dns.update.spi.DnsUpdateProvider;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.StreamSupport;

@Service
public class DnsUpdates {

    private final IterableProvider<DnsUpdateProvider> providers;
    private final Configuration configuration;
    private final Logger logger;

    @Inject
    public DnsUpdates(IterableProvider<DnsUpdateProvider> providers, Configuration configuration, Logger logger) {
        this.providers = providers;
        this.configuration = configuration;
        this.logger = logger;
    }

    public CompletionStage<Void> updateDns(String zone, List<Record> dnsUpdates) {
        return StreamSupport.stream(providers.handleIterator().spliterator(), false).map(h ->
                {
                    logger.info("Updating DNS records for provider:" + h.getActiveDescriptor().getName());
                    return h.getService().updateDns(zone, dnsUpdates);
                }).reduce((c1, c2) -> c1.thenCombine(c2, (v1, v2) -> v1))
                .stream().findFirst()
                .orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No DNS update provider available")));
    }

    public CompletionStage<Void> updateDns(List<Record> dnsUpdates) {
        return updateDns(configuration.getString("instance.domain").orElse("local"), dnsUpdates);
    }
}
