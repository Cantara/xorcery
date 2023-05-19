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
package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.util.Sockets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HostsConfigurationLookup
        implements DnsLookup {
    private final ObjectNode hosts;
    private final DnsClientConfiguration dnsClientConfiguration;

    public HostsConfigurationLookup(Configuration configuration) {
        dnsClientConfiguration = new DnsClientConfiguration(configuration.getConfiguration("dns.client"));
        hosts = dnsClientConfiguration.getHosts();
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {

        String host = uri.getHost();
        if (host == null) {
            host = uri.getAuthority();
        }
        if (host != null) {
            try {
                Iterable<String> hostNames = getHostNames(host, uri.getScheme());
                for (String hostName : hostNames) {
                    JsonNode lookup = hosts.get(hostName);
                    if (lookup instanceof TextNode) {
                        URI newUri = toURI(lookup.textValue(), uri);
                        return CompletableFuture.completedFuture(List.of(newUri));
                    } else if (lookup instanceof ArrayNode an) {
                        List<URI> addresses = new ArrayList<>();
                        for (JsonNode jsonNode : an) {
                            URI newUri = toURI(jsonNode.textValue(), uri);
                            addresses.add(newUri);
                        }
                        return CompletableFuture.completedFuture(addresses);
                    }
                }
            } catch (URISyntaxException e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    private Iterable<String> getHostNames(String host, String scheme) {
        if (host.contains(".") && !scheme.equals("srv"))
        {
            return List.of(host);
        } else
        {
            List<String> hostNames = new ArrayList<>();
            hostNames.add(host);
            for (Name domainName : dnsClientConfiguration.getSearchDomains()) {
                try {
                    hostNames.add(Name.fromString(host, domainName).toString(true));
                } catch (TextParseException e) {
                    // Ignore
                }
            }
            return hostNames;
        }
    }

    private URI toURI(String jsonText, URI uri) throws URISyntaxException {
        if (jsonText.contains("://"))
        {
            return URI.create(jsonText);
        } else
        {
            InetSocketAddress inetSocketAddress = Sockets.getInetSocketAddress(jsonText, uri.getPort());
            return new URI(uri.getScheme(), uri.getUserInfo(), inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        }
    }
}
