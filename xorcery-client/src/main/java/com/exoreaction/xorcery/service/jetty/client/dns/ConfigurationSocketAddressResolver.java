package com.exoreaction.xorcery.service.jetty.client.dns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationSocketAddressResolver
        implements SocketAddressResolver {
    private ObjectNode hosts;
    private SocketAddressResolver delegate;

    public ConfigurationSocketAddressResolver(ObjectNode hosts, SocketAddressResolver delegate) {
        this.hosts = hosts;
        this.delegate = delegate;
    }

    @Override
    public void resolve(String host, int port, Promise<List<InetSocketAddress>> promise) {

        JsonNode lookup = hosts.get(host);
        if (lookup instanceof TextNode) {
            promise.succeeded(List.of(new InetSocketAddress(lookup.textValue(), port)));
        } else if (lookup instanceof ArrayNode an) {
            List<InetSocketAddress> addresses = new ArrayList<>();
            for (JsonNode jsonNode : an) {
                addresses.add(new InetSocketAddress(jsonNode.textValue(), port));
            }
            promise.succeeded(addresses);
        } else {
            delegate.resolve(host, port, promise);
        }
    }
}
