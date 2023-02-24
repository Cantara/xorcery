package com.exoreaction.xorcery.service.dns.registration;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
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
import java.util.*;

@Service(name = "dns.registration")
@RunLevel(20)
public class DnsRegistrationService
        implements PreDestroy {

    private final Resolver resolver;
    private final Name zone;
    private final DnsRecords dnsRecords;

    @Inject
    public DnsRegistrationService(DnsRecords dnsRecords,
                                  Configuration configuration) throws IOException {
        this.dnsRecords = dnsRecords;

        resolver = getResolver(configuration);

        Message msg = new Message();
        msg.getHeader().setOpcode(Opcode.UPDATE);
        StandardConfiguration standardConfiguration = () -> configuration;

        String domain = configuration.getString("dns.domain").orElse("xorcery.test");
        zone = Name.fromConstantString(domain + ".");

        Record soa = Record.newRecord(zone, Type.SOA, DClass.IN);
        msg.addRecord(soa, Section.ZONE);

        for (Record record : dnsRecords.getRecords()) {
            msg.addRecord(record, Section.UPDATE);
        }

        LogManager.getLogger(getClass()).debug("Update:" + msg);
        Message response = resolver.send(msg);
        LogManager.getLogger(getClass()).debug("Response:" + response);
    }

    private static Resolver getResolver(Configuration configuration) {

        Resolver resolver = configuration.getListAs("dns.nameservers", JsonNode::textValue)
                .flatMap(hosts ->
                {
                    if (hosts.isEmpty()) {
                        return Optional.empty();
                    } else {
                        List<Resolver> resolvers = new ArrayList<>();
                        for (String nameserver : hosts) {
                            return Optional.of(new SimpleResolver(Sockets.getInetSocketAddress(nameserver, 53)));
                        }
                        return Optional.empty();
                    }
                })
                .orElseGet(() ->
                {
                    List<InetSocketAddress> servers = ResolverConfig.getCurrentConfig().servers();
                    return new SimpleResolver(servers.get(0));
                });

        configuration.getString("dns.registration.key.name").ifPresent(keyname ->
        {
            configuration.getString("dns.registration.key.secret").ifPresent(keydata ->
            {
                Name algo = configuration.getString("dns.registration.key.algorithm")
                        .map(Name::fromConstantString)
                        .orElse(TSIG.HMAC_MD5);
                resolver.setTSIGKey(new TSIG(algo, keyname, keydata));
            });
        });

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
