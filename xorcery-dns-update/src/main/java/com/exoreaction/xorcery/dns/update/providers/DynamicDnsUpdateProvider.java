package com.exoreaction.xorcery.dns.update.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.update.spi.DnsUpdateProvider;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.dns.client.providers.DnsClientConfiguration;
import com.exoreaction.xorcery.util.Sockets;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Service(name = "dns.dyndns")
@ContractsProvided(DnsUpdateProvider.class)
public class DynamicDnsUpdateProvider
        implements DnsUpdateProvider {

    private final Resolver resolver;
    private final Logger logger = LogManager.getLogger(getClass());
    private final DynamicDnsConfiguration dynamicDnsConfiguration;
    private final DnsClientConfiguration dnsClientConfiguration;

    @Inject
    public DynamicDnsUpdateProvider(Configuration configuration, Secrets secrets) {
        dynamicDnsConfiguration = new DynamicDnsConfiguration(configuration.getConfiguration("dns.dyndns"));
        dnsClientConfiguration = new DnsClientConfiguration(configuration.getConfiguration("dns.client"));

        resolver = getResolver();

        dynamicDnsConfiguration.getKey().ifPresent(key ->
        {
            try {
                Name algo = Name.fromConstantString(key.getAlgorithm());
                resolver.setTSIGKey(new TSIG(algo, key.getName(), secrets.getSecretString(key.getSecretName())));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

    }

    @Override
    public CompletionStage<Void> updateDns(String zone, List<Record> dnsUpdates) {

        Name zoneName = Name.fromConstantString(zone + ".");

        Message msg = Message.newUpdate(zoneName);

        for (Record record : dnsUpdates) {
            msg.addRecord(record, Section.UPDATE);
        }

        logger.debug("Update:" + msg);
        return resolver.sendAsync(msg).thenApply(response ->
        {
            LogManager.getLogger(getClass()).debug("Response:" + response);
            return null;
        });
    }

    private Resolver getResolver() {
        Resolver resolver = dnsClientConfiguration.getNameServers()
                .flatMap(hosts ->
                {
                    if (hosts.isEmpty()) {
                        return Optional.empty();
                    } else {
                        List<Resolver> resolvers = new ArrayList<>();
                        for (String nameserver : hosts) {
                            SimpleResolver simpleResolver = new SimpleResolver(Sockets.getInetSocketAddress(nameserver, 53));
                            resolvers.add(simpleResolver);
                        }
                        if (resolvers.size() == 1) {
                            return Optional.of(resolvers.get(0));
                        } else {
                            return Optional.<Resolver>of(new ExtendedResolver(resolvers));
                        }
                    }
                })
                .orElseGet(() ->
                {
                    List<InetSocketAddress> servers = ResolverConfig.getCurrentConfig().servers();
                    return new SimpleResolver(servers.get(0));
                });


        resolver.setTimeout(dynamicDnsConfiguration.getTtl());

        return resolver;
    }
}
