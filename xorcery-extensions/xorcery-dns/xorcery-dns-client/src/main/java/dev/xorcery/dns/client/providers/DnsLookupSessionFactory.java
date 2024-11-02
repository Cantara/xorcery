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
package dev.xorcery.dns.client.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.net.Sockets;
import org.xbill.DNS.Cache;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.hosts.HostsFileParser;
import org.xbill.DNS.lookup.LookupSession;

import java.util.ArrayList;
import java.util.List;

public class DnsLookupSessionFactory {
    private final LookupSession lookupSession;

    public DnsLookupSessionFactory(Configuration configuration) {

        DnsClientConfiguration dnsClientConfiguration = new DnsClientConfiguration(configuration.getConfiguration("dns.client"));

        Resolver resolver = dnsClientConfiguration.getNameServers()
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

        resolver.setTimeout(dnsClientConfiguration.getTimeout());
        resolver.setTCP(dnsClientConfiguration.getForceTCP());

        lookupSession = LookupSession.builder()
                .searchPath(dnsClientConfiguration.getSearchDomains())
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
