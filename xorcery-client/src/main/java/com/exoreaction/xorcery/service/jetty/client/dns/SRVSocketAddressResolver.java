package com.exoreaction.xorcery.service.jetty.client.dns;

import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.xbill.DNS.Section.ADDITIONAL;
import static org.xbill.DNS.Section.ANSWER;

public class SRVSocketAddressResolver
        implements SocketAddressResolver {
    private final Resolver resolver;
    private final SocketAddressResolver delegate;

    private final Map<String, SRVLookup> serviceLookups = new ConcurrentHashMap<>();
    private final Map<InetSocketAddress, AtomicInteger> serverRequests = new ConcurrentHashMap<>();

    public SRVSocketAddressResolver(List<String> nameservers, SocketAddressResolver delegate) throws UnknownHostException {
        if (nameservers.isEmpty()) {
            resolver = new ExtendedResolver();
        } else {
            List<Resolver> resolvers = new ArrayList<>();
            for (String nameserver : nameservers) {
                String[] hostPort = nameserver.split(":");
                String host = hostPort[0];
                int port = hostPort.length == 2 ? Integer.parseInt(hostPort[1]) : 53;
                resolvers.add(new SimpleResolver(new InetSocketAddress(host, port)));
            }
            resolver = new ExtendedResolver(resolvers);
        }
        this.delegate = delegate;
    }

    @Override
    public void resolve(String host, int port, Promise<List<InetSocketAddress>> promise) {

        try {
            SRVLookup serviceLookup = serviceLookups.compute(host, (h, oldLookup) ->
            {
                long now = System.currentTimeMillis();
                if (oldLookup != null && oldLookup.expiresOn() > now) {
                    return oldLookup;
                }
                try {
                    Record queryRecord = Record.newRecord(Name.fromString(host+"."), Type.SRV, DClass.IN);
                    Message queryMessage = Message.newQuery(queryRecord);
                    Message queryResponse = resolver.send(queryMessage);
                    long minTTL = Long.MAX_VALUE;
                    for (Record record : queryResponse.getSection(ANSWER)) {
                        minTTL = Math.min(minTTL, record.getTTL());
                    }
                    for (Record record : queryResponse.getSection(ADDITIONAL)) {
                        if (record instanceof ARecord)
                            minTTL = Math.min(minTTL, record.getTTL());
                    }
                    minTTL = 1;
                    return new SRVLookup(queryResponse, now + minTTL * 1000);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            Message queryResponse = serviceLookup.message();
            if (queryResponse.getSection(ANSWER).isEmpty()) {
                delegate.resolve(host, port, promise);
            } else {
                List<ServerEntry> servers = new ArrayList<>();
                for (Record record : queryResponse.getSection(ANSWER)) {
                    SRVRecord srvRecord = (SRVRecord) record;
                    String serviceHost = queryResponse.getSection(Section.ADDITIONAL)
                            .stream()
                            .filter(r -> r.getName().equals(srvRecord.getTarget())).findFirst()
                            .map(Record::getName)
                            .orElse(srvRecord.getTarget())
                            .toString(true);
                    InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(serviceHost), srvRecord.getPort());
                    servers.add(new ServerEntry(address, srvRecord.getPriority(), serverRequests.computeIfAbsent(address, h -> new AtomicInteger(1)).get(), srvRecord.getWeight()));
                }

                if (!servers.isEmpty()) {
                    // Sort based on priority and weight
                    Collections.sort(servers);
                    System.out.println("Sorted:" + servers);
                    serverRequests.get(servers.get(0).address).incrementAndGet();
                }

                promise.succeeded(servers.stream().map(ServerEntry::address).collect(Collectors.toList()));
            }
        } catch (Throwable e) {
            promise.failed(e);
        }
    }

    record ServerEntry(InetSocketAddress address, int priority, int requests, int weight)
            implements Comparable<ServerEntry> {
        @Override
        public int compareTo(ServerEntry entry) {

            if (entry.priority == priority) {
                // Same priority, use weight and the nr of requests so far
                return entry.weight / entry.requests - weight / requests;
            }

            return entry.priority - priority;
        }
    }

    record SRVLookup(Message message, long expiresOn) {
    }

    ;
}
