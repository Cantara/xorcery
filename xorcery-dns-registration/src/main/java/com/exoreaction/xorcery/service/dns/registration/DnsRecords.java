package com.exoreaction.xorcery.service.dns.registration;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.util.*;

@Service
public class DnsRecords {

    private final List<Record> records = new ArrayList<>();

    @Inject
    public DnsRecords(Configuration configuration, ServiceResourceObjects serviceResourceObjects) throws TextParseException {

        StandardConfiguration standardConfiguration = () -> configuration;
        InetAddress targetIp = standardConfiguration.getIp();

        Name target;
        Name zone;

        if (standardConfiguration.getHost().contains(".")) {
            target = Name.fromConstantString(standardConfiguration.getHost() + ".");
        } else {
            String domain = configuration.getString("dns.registration.domain").orElse("xorcery.test");
            target = Name.fromConstantString(standardConfiguration.getHost() + "." + domain + ".");
        }
        zone = new Name(target, 1);

        int httpPort = configuration.getInteger("jetty.server.http.port").orElse(80);
        Optional<Integer> httpsPort = configuration.getBoolean("jetty.server.ssl.enabled").flatMap(enabled ->
                enabled ? configuration.getInteger("jetty.server.ssl.port") : Optional.empty());

        // Self A Record
        ARecord selfARecord = new ARecord(target, DClass.IN, 60, targetIp);
        records.add(selfARecord);

        // SRV Records
        String serverHttpType = "_http._tcp." + zone.toString(false);
        Name serverName = Name.fromString(serverHttpType);
        int weight = configuration.getInteger("jetty.server.srv.weight").orElse(1);
        int priority = configuration.getInteger("jetty.server.srv.priority").orElse(1);
        records.add(new SRVRecord(serverName, DClass.IN, 60, priority, weight, httpPort, target));
        if (httpsPort.isPresent()) {
            String serverHttpsType = "_https._tcp." + zone.toString(false);
            Name httpsServerName = Name.fromString(serverHttpsType);
            SRVRecord httpsSrvRecord = new SRVRecord(httpsServerName, DClass.IN, 60, priority, weight, httpsPort.get(), target);
            records.add(httpsSrvRecord);
        }

        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {

            String type = serviceResource.getServiceIdentifier().resourceObjectIdentifier().getType();
            weight = configuration.getInteger(type + ".srv.weight").orElse(1);
            priority = configuration.getInteger(type + ".srv.priority").orElse(1);

            String serviceHttpType = "_" + type + "._sub._http._tcp." + zone.toString(false);
            Name serviceName = Name.fromString(serviceHttpType);

            List<String> props = new ArrayList<>();
            for (Map.Entry<String, String> attribute : serviceResource.getAttributes().attributes().toMap().entrySet()) {
                props.add(attribute.getKey() + "=" + attribute.getValue());
            }
            for (Link link : serviceResource.resourceObject().getLinks().getLinks()) {
                props.add(link.rel() + "=" + link.getHref());
            }
            TXTRecord txtRecord = new TXTRecord(serviceName, DClass.IN, 60, props);
            records.add(txtRecord);

            SRVRecord srvRecord = new SRVRecord(serviceName, DClass.IN, 60, priority, weight, httpPort, target);
            records.add(srvRecord);

            if (httpsPort.isPresent()) {
                String serviceHttpsType = "_" + type + "._sub._https._tcp." + zone.toString(false);
                Name httpsServiceName = Name.fromString(serviceHttpsType);
                SRVRecord httpsSrvRecord = new SRVRecord(httpsServiceName, DClass.IN, 60, priority, weight, httpsPort.get(), target);
                records.add(httpsSrvRecord);

                TXTRecord httpsTxtRecord = new TXTRecord(httpsServiceName, DClass.IN, 60, props);
                records.add(httpsTxtRecord);
            }
        }

        // Service A Records
        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {

            String type = serviceResource.getServiceIdentifier().resourceObjectIdentifier().getType();

            String name = type + "." + zone.toString(false);

            Name aName = Name.fromString(name);
            ARecord aRecord = new ARecord(aName, DClass.IN, 60, targetIp);
            records.add(aRecord);
        }
    }

    public List<Record> getRecords() {
        return records;
    }
}
