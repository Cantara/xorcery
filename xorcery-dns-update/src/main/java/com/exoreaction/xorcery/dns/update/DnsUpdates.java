package com.exoreaction.xorcery.dns.update;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.update.spi.DnsUpdateProvider;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;

import java.util.List;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
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
