package com.exoreaction.xorcery.service.dns.registration;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

@Service(name = "dns.registration")
@RunLevel(20)
public class DnsRegistrationService
        implements PreDestroy {

    private final Resolver resolver;
    private final Name zone;
    private final Name target;
    private Configuration configuration;
    private List<Record> registeredRecords = new ArrayList<>();

    @Inject
    public DnsRegistrationService(
            Configuration configuration,
            ServiceResourceObjects serviceResourceObjects) throws IOException {
        this.configuration = configuration;

        resolver = getResolver(configuration);

        Message msg = new Message();
        msg.getHeader().setOpcode(Opcode.UPDATE);
        StandardConfiguration standardConfiguration = () -> configuration;

        if (standardConfiguration.getHost().contains(".")) {
            target = Name.fromConstantString(standardConfiguration.getHost() + ".");
        } else {
            String domain = configuration.getString("dns.domain").orElse("xorcery.test");
            target = Name.fromConstantString(standardConfiguration.getHost() + "." + domain + ".");
        }
        zone = new Name(target, 1);

        Record soa = Record.newRecord(zone, Type.SOA, DClass.IN);
        msg.addRecord(soa, Section.ZONE);

        // A Record
        msg.addRecord(new ARecord(target, DClass.IN, 60, InetAddress.getLocalHost()), Section.UPDATE);

        // SRV Records
        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {

            String type = serviceResource.getServiceIdentifier().resourceObjectIdentifier().getType();
            int weight = configuration.getInteger(type + ".srv.weight").orElse(1);
            int priority = configuration.getInteger(type + ".srv.priority").orElse(1);

            Map<String, Object> properties = new HashMap<>(serviceResource.getAttributes().attributes().toMap());
            for (Link link : serviceResource.resourceObject().getLinks().getLinks()) {
                properties.put(link.rel(), link.getHref());
            }

            String serviceType = "_" + type + "._tcp." + zone.toString(false);

            Name serviceName = Name.fromString(serviceType);
            SRVRecord srvRecord = new SRVRecord(serviceName, DClass.IN, 60, priority, weight, standardConfiguration.getServerUri().getPort(), target);
            msg.addRecord(srvRecord, Section.UPDATE);
            registeredRecords.add(srvRecord);
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

            // Remove A record for this server
            msg.addRecord(Record.newRecord(target, Type.A, DClass.ANY, 0), Section.UPDATE);

            // Remove SRV records for this server
            for (Record registeredRecord : registeredRecords) {
                msg.addRecord(Record.newRecord(registeredRecord.getName(), Type.SRV, DClass.NONE, 0, registeredRecord.rdataToWireCanonical()), Section.UPDATE);
// Delete all records for this service                msg.addRecord(Record.newRecord(registeredRecord.getName(), Type.SRV, DClass.ANY, 0), Section.UPDATE);
            }

            LogManager.getLogger(getClass()).debug("Delete:" + msg);
            Message response = resolver.send(msg);
            LogManager.getLogger(getClass()).debug("Response:" + response);
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Exception deregistering from DNS", e);
        }
    }
}
