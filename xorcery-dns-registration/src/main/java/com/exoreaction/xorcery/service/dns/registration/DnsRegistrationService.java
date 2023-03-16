package com.exoreaction.xorcery.service.dns.registration;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.service.dns.client.DnsClientConfiguration;
import com.exoreaction.xorcery.util.Sockets;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;

@Service(name = "dns.registration")
@RunLevel(20)
public class DnsRegistrationService
        implements PreDestroy {

    private final Resolver resolver;
    private final Name zone;
    private final DnsRecords dnsRecords;
    private final DnsRegistrationConfiguration dnsRegistrationConfiguration;
    private final DnsClientConfiguration dnsClientConfiguration;

    @Inject
    public DnsRegistrationService(DnsRecords dnsRecords,
                                  Configuration configuration) throws IOException {
        this.dnsRecords = dnsRecords;

        resolver = getResolver();

        InstanceConfiguration instanceConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        dnsRegistrationConfiguration = new DnsRegistrationConfiguration(configuration.getConfiguration("dns.registration"));
        dnsClientConfiguration = new DnsClientConfiguration(configuration.getConfiguration("dns.client"));

        String domain = instanceConfiguration.getDomain();
        zone = Name.fromConstantString(domain + ".");

        Message msg = Message.newUpdate(zone);
//        msg.getHeader().setOpcode(Opcode.UPDATE);

//        Record soa = Record.newRecord(zone, Type.SOA, DClass.IN);
//        msg.addRecord(soa, Section.ZONE);

        for (Record record : dnsRecords.getRecords()) {
            msg.addRecord(record, Section.UPDATE);
        }

        LogManager.getLogger(getClass()).debug("Update:" + msg);
        Message response = resolver.send(msg);
        LogManager.getLogger(getClass()).debug("Response:" + response);
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
                        if (resolvers.size()==1)
                        {
                            return Optional.of(resolvers.get(0));
                        } else
                        {
                            return Optional.<Resolver>of(new ExtendedResolver(resolvers));
                        }
                    }
                })
                .orElseGet(() ->
                {
                    List<InetSocketAddress> servers = ResolverConfig.getCurrentConfig().servers();
                    return new SimpleResolver(servers.get(0));
                });

        dnsRegistrationConfiguration.getKey().ifPresent(key ->
        {
            Name algo = Name.fromConstantString(key.getAlgorithm());
            resolver.setTSIGKey(new TSIG(algo, key.getName(), key.getSecret()));
        });

        resolver.setTimeout(dnsRegistrationConfiguration.getTtl());

        return resolver;
    }

    @Override
    public void preDestroy() {
        try {
            Message msg = new Message();
            msg.getHeader().setOpcode(Opcode.UPDATE);
            Record soa = Record.newRecord(zone, Type.SOA, DClass.IN);
            msg.addRecord(soa, Section.ZONE);

/*
            // Remove A record for this server
            msg.addRecord(Record.newRecord(target, Type.A, DClass.ANY, 0), Section.UPDATE);
*/

            // Remove SRV records for this server
            for (Record registeredRecord : dnsRecords.getRecords()) {
                msg.addRecord(Record.newRecord(registeredRecord.getName(), registeredRecord.getType(), DClass.NONE, 0, registeredRecord.rdataToWireCanonical()), Section.UPDATE);
            }

            LogManager.getLogger(getClass()).debug("Delete:" + msg);
            Message response = resolver.send(msg);
            LogManager.getLogger(getClass()).debug("Response:" + response);
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Exception deregistering from DNS", e);
        }
    }
}
