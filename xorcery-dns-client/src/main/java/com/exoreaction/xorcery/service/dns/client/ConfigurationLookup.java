package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.util.Sockets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConfigurationLookup
        implements DnsLookup {
    private final ObjectNode hosts;

    public ConfigurationLookup(Configuration configuration) {
        hosts = (ObjectNode) configuration.getJson("dns.hosts").orElseGet(JsonNodeFactory.instance::objectNode);
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {

        String host = uri.getHost();
        if (host != null) {
            try {
                JsonNode lookup = hosts.get(uri.getHost());
                if (lookup instanceof TextNode) {
                    InetSocketAddress inetSocketAddress = Sockets.getInetSocketAddress(lookup.textValue(), uri.getPort());
                    URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                    return CompletableFuture.completedFuture(List.of(newUri));
                } else if (lookup instanceof ArrayNode an) {
                    List<URI> addresses = new ArrayList<>();
                    for (JsonNode jsonNode : an) {
                        InetSocketAddress inetSocketAddress = Sockets.getInetSocketAddress(lookup.textValue(), uri.getPort());
                        URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                        addresses.add(newUri);
                    }
                    return CompletableFuture.completedFuture(addresses);
                }
            } catch (URISyntaxException e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
