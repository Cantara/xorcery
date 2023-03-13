package com.exoreaction.xorcery.service.dns.registration;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.time.Duration;
import java.util.*;

@Service
public class DnsRecords {

    private final List<Record> records = new ArrayList<>();

    @Inject
    public DnsRecords(Configuration configuration, ServiceResourceObjects serviceResourceObjects) throws TextParseException {

        InstanceConfiguration standardConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        InetAddress targetIp = standardConfiguration.getIp();

        Name target;
        Name zone;

        int ttl = configuration.getString("dns.registration.ttl")
                .map(s -> "PT"+s)
                .map(Duration::parse)
                .map(Duration::getSeconds)
                .orElse(60L)
                .intValue();

        if (standardConfiguration.getHost().contains(".")) {
            target = Name.fromConstantString(standardConfiguration.getHost() + ".");
        } else {
            String domain = configuration.getString("domain").orElse("xorcery.test");
            target = Name.fromConstantString(standardConfiguration.getHost() + "." + domain + ".");
        }
        zone = new Name(target, 1);

        int httpPort = configuration.getInteger("jetty.server.http.port").orElse(80);
        Optional<Integer> httpsPort = configuration.getBoolean("jetty.server.ssl.enabled").flatMap(enabled ->
                enabled ? configuration.getInteger("jetty.server.ssl.port") : Optional.empty());

        // Self A Record
        ARecord selfARecord = new ARecord(target, DClass.IN, ttl, targetIp);
        records.add(selfARecord);

        // SRV Records
        String serverHttpType = "_http._tcp." + zone.toString(false);
        Name serverName = Name.fromString(serverHttpType);
        int weight = configuration.getInteger("jetty.server.srv.weight").orElse(1);
        int priority = configuration.getInteger("jetty.server.srv.priority").orElse(1);
        records.add(new SRVRecord(serverName, DClass.IN, ttl, priority, weight, httpPort, target));
        if (httpsPort.isPresent()) {
            String serverHttpsType = "_https._tcp." + zone.toString(false);
            Name httpsServerName = Name.fromString(serverHttpsType);
            SRVRecord httpsSrvRecord = new SRVRecord(httpsServerName, DClass.IN, ttl, priority, weight, httpsPort.get(), target);
            records.add(httpsSrvRecord);
        }

        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {

            // HTTP(S) SRV records
            String type = serviceResource.getServiceIdentifier().resourceObjectIdentifier().getType();
            weight = configuration.getInteger(type + ".srv.weight").orElse(1);
            priority = configuration.getInteger(type + ".srv.priority").orElse(1);
            int srvTtl = configuration.getInteger(type + ".srv.ttl").orElse(ttl);

            {
                String serviceHttpType = "_" + type + "._sub._http._tcp." + zone.toString(false);
                Name serviceName = Name.fromString(serviceHttpType);

                List<String> props = new ArrayList<>();
                for (Map.Entry<String, String> attribute : JsonElement.toMap(serviceResource.getAttributes().attributes().json(), JsonNode::asText).entrySet()) {
                    props.add(attribute.getKey() + "=" + attribute.getValue());
                }
                boolean hasHttpEndpoints = false;
                for (Link link : serviceResource.resourceObject().getLinks().getLinks()) {
                    if (link.getHref().startsWith("http")) {
                        props.add(link.rel() + "=" + link.getHrefAsUri().getPath());
                        hasHttpEndpoints = true;
                    }
                }

                if (hasHttpEndpoints) {
                    TXTRecord txtRecord = new TXTRecord(serviceName, DClass.IN, srvTtl, props);
                    records.add(txtRecord);

                    SRVRecord srvRecord = new SRVRecord(serviceName, DClass.IN, srvTtl, priority, weight, httpPort, target);
                    records.add(srvRecord);

                    if (httpsPort.isPresent()) {
                        String serviceHttpsType = "_" + type + "._sub._https._tcp." + zone.toString(false);
                        Name httpsServiceName = Name.fromString(serviceHttpsType);
                        SRVRecord httpsSrvRecord = new SRVRecord(httpsServiceName, DClass.IN, srvTtl, priority, weight, httpsPort.get(), target);
                        records.add(httpsSrvRecord);

                        TXTRecord httpsTxtRecord = new TXTRecord(httpsServiceName, DClass.IN, srvTtl, props);
                        records.add(httpsTxtRecord);
                    }
                }
            }

            {
                String serviceWsType = "_" + type + "._sub._ws._tcp." + zone.toString(false);
                Name serviceName = Name.fromString(serviceWsType);

                List<String> props = new ArrayList<>();
                for (Map.Entry<String, String> attribute : JsonElement.toMap(serviceResource.getAttributes().attributes().json(), JsonNode::asText).entrySet()) {
                    props.add(attribute.getKey() + "=" + attribute.getValue());
                }
                boolean hasWsEndpoints = false;
                for (Link link : serviceResource.resourceObject().getLinks().getLinks()) {
                    if (link.getHref().startsWith("ws")) {
                        props.add(link.rel() + "=" + link.getHrefAsUri().getPath());
                        hasWsEndpoints = true;
                    }
                }

                if (hasWsEndpoints) {
                    TXTRecord txtRecord = new TXTRecord(serviceName, DClass.IN, srvTtl, props);
                    records.add(txtRecord);

                    SRVRecord srvRecord = new SRVRecord(serviceName, DClass.IN, srvTtl, priority, weight, httpPort, target);
                    records.add(srvRecord);

                    if (httpsPort.isPresent()) {
                        String serviceWssType = "_" + type + "._sub._wss._tcp." + zone.toString(false);
                        Name wssServiceName = Name.fromString(serviceWssType);
                        SRVRecord wssSrvRecord = new SRVRecord(wssServiceName, DClass.IN, srvTtl, priority, weight, httpsPort.get(), target);
                        records.add(wssSrvRecord);

                        TXTRecord wssTxtRecord = new TXTRecord(wssServiceName, DClass.IN, srvTtl, props);
                        records.add(wssTxtRecord);
                    }
                }
            }
        }

        // Service A Records
        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {

            String type = serviceResource.getServiceIdentifier().resourceObjectIdentifier().getType();

            String name = type + "." + zone.toString(false);

            Name aName = Name.fromString(name);
            ARecord aRecord = new ARecord(aName, DClass.IN, ttl, targetIp);
            records.add(aRecord);
        }
    }

    public List<Record> getRecords() {
        return records;
    }
}
