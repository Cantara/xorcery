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

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DnsRecords {

    private final Configuration configuration;
    private final ServiceResourceObjects serviceResourceObjects;

    @Inject
    public DnsRecords(Configuration configuration, ServiceResourceObjects serviceResourceObjects) throws TextParseException {
        this.configuration = configuration;
        this.serviceResourceObjects = serviceResourceObjects;
    }

    public List<Record> getRecords() throws IOException {

        List<Record> records = new ArrayList<>();

        InstanceConfiguration instanceConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        InetAddress targetIp = instanceConfiguration.getIp();

        Name target;
        Name zone;

        int ttl = configuration.getString("dns.registration.ttl")
                .map(s -> "PT" + s)
                .map(Duration::parse)
                .map(Duration::getSeconds)
                .orElse(60L)
                .intValue();

        if (instanceConfiguration.getHost().contains(".")) {
            target = Name.fromConstantString(instanceConfiguration.getHost() + ".");
        } else {
            String domain = Optional.ofNullable(instanceConfiguration.getDomain()).orElse("local");
            target = Name.fromConstantString(instanceConfiguration.getHost() + "." + domain + ".");
        }
        zone = new Name(target, 1);

        Optional<Integer> httpPort = configuration.getBoolean("jetty.server.http.enabled").flatMap(enabled ->
                enabled ? configuration.getInteger("jetty.server.http.port") : Optional.empty());
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
        if (httpPort.isPresent())
        {
            records.add(new SRVRecord(serverName, DClass.IN, ttl, priority, weight, httpPort.get(), target));
        }
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
                    if (httpPort.isPresent()) {
                        SRVRecord srvRecord = new SRVRecord(serviceName, DClass.IN, srvTtl, priority, weight, httpPort.get(), target);
                        records.add(srvRecord);

                        TXTRecord txtRecord = new TXTRecord(serviceName, DClass.IN, srvTtl, props);
                        records.add(txtRecord);
                    }

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

                    if (httpPort.isPresent())
                    {
                        SRVRecord srvRecord = new SRVRecord(serviceName, DClass.IN, srvTtl, priority, weight, httpPort.get(), target);
                        records.add(srvRecord);
                        TXTRecord txtRecord = new TXTRecord(serviceName, DClass.IN, srvTtl, props);
                        records.add(txtRecord);
                    }

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

        return records;
    }
}
