package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.util.Sockets;
import com.fasterxml.jackson.databind.JsonNode;
import org.xbill.DNS.*;
import org.xbill.DNS.hosts.HostsFileParser;
import org.xbill.DNS.lookup.LookupSession;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DnsLookupSessionFactory {
    private final LookupSession lookupSession;

    public DnsLookupSessionFactory(Configuration configuration) {

        Resolver resolver = configuration.getListAs("dns.client.nameservers", JsonNode::textValue)
                .map(hosts ->
                {
                    if (hosts.isEmpty()) {
                        return new ExtendedResolver();
                    } else {
                        List<Resolver> resolvers = new ArrayList<>();
                        for (String nameserver : hosts) {
                            resolvers.add(new SimpleResolver(Sockets.getInetSocketAddress(nameserver, 53)));
                        }
                        if (resolvers.size() == 1) {
                            return resolvers.get(0);
                        } else {
                            return new ExtendedResolver(resolvers);
                        }
                    }
                })
                .orElseGet(ExtendedResolver::new);

        configuration.getString("dns.client.timeout")
                .map(s -> "PT" + s)
                .map(Duration::parse)
                .ifPresent(resolver::setTimeout);

        lookupSession = LookupSession.builder()
                .searchPath(configuration.getListAs("dns.client.search", json -> {
                    try {
                        return Name.fromString(json.textValue());
                    } catch (TextParseException e) {
                        throw new UncheckedIOException(e);
                    }
                }).orElseGet(Collections::emptyList))
                .cache(new Cache())
                .resolver(resolver)
                .hostsFileParser(new HostsFileParser())
                .build();
    }

    public LookupSession provide() {
        return lookupSession;
    }

    public void dispose(LookupSession instance) {
    }
}
