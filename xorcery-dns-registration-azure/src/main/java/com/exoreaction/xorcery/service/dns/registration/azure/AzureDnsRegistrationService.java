package com.exoreaction.xorcery.service.dns.registration.azure;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.service.dns.registration.DnsRecords;
import com.exoreaction.xorcery.service.dns.registration.azure.model.*;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TXTRecord;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.stream.Stream;

@Service(name = "dns.registration.azure")
@RunLevel(20)
public class AzureDnsRegistrationService implements PreDestroy {
    private final Logger logger = LogManager.getLogger(getClass());
    private final AzureConfiguration azureConfiguration;
    private final InstanceConfiguration instanceConfiguration;
    private final DnsRecords dnsRecords;
    private final AzureAuthenticationClient loginClient;
    private final String zoneString;

    @Inject
    public AzureDnsRegistrationService(HttpClient httpClient, Configuration configuration, DnsRecords dnsRecords) {
        this.azureConfiguration = () -> configuration.getConfiguration("dns.registration.azure");
        this.instanceConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        this.zoneString = Name.fromConstantString(instanceConfiguration.configuration().getString("domain") + ".").toString();
        this.dnsRecords = dnsRecords;
        // get required configuration properties
        this.loginClient = new AzureAuthenticationClient(httpClient, this.azureConfiguration);
        var azureDnsClient = loginClient.loginDns()
                .toCompletableFuture()
                .join();

        // register
        final var existingRecords = azureDnsClient.listRecords(null)
                .thenApply(azureDnsResponse -> {
                    if (azureDnsResponse.hasNext()) {
                        // do "pagination" in case there's more than 100 records.
                        // azureDnsClient.listRecords(azureDnsResponse.getNextLink())
                        // should this logic be placed in the client itself?
                    }
                    return azureDnsResponse;
                })
                .toCompletableFuture()
                .join();

        System.out.println(existingRecords.json().toPrettyString());

        dnsRecords.getRecords()
                .forEach(record -> {
                    var name = getNameWithoutZone(record.getName().toString());
                    var existingRecord = existingRecords.list()
                            .stream()
                            .filter(adr ->
                                adr.getName().equals(name) &&
                                    compareType(adr.getType(), record.getType()))
                            .findFirst();
                    var requestBuilder = new AzureDnsRecord.Builder();
                    var propertiesBuilder = new AzureDnsRecordProperties.Builder();
                    propertiesBuilder.ttl((int) record.getTTL());
                    propertiesBuilder.metadata(new AzureDnsMetadata.Builder()
                        .metadata("created-by", "xorcery")
                        .build());
                    var type = toType(record.getType());

                    switch (type) {
                        case A -> {
                            var rec = (ARecord) record;
                            var aRecords = new AzureDnsARecords.Builder();
                            var aRecord = new AzureDnsARecord.Builder();
                            var ip = rec.getAddress() instanceof Inet4Address address ? address : (Inet6Address) rec.getAddress();
                            aRecord.setIP(ip.getHostAddress());
                            aRecords.aRecord(aRecord.build());
                            propertiesBuilder.aRecords(aRecords.build());
                        }
                        case TXT -> {
                            var rec = (TXTRecord) record;
                            var txtRecords = new AzureDnsTXTRecords.Builder();
                            var txtRecord = new AzureDnsTXTRecord.Builder();
                            rec.getStrings().forEach(txtRecord::value);
                            txtRecords.txtRecord(txtRecord.build());
                            propertiesBuilder.txtRecords(txtRecords.build());
                        }
                        case SRV -> {
                            var rec = (SRVRecord) record;
                            var srvRecords = new AzureDnsSRVRecords.Builder();
                            var srvRecord = new AzureDnsSRVRecord.Builder();
                            srvRecord.port(rec.getPort());
                            srvRecord.weight(rec.getWeight());
                            srvRecord.priority(rec.getPriority());
                            srvRecord.target(rec.getTarget().toString(true));
                            srvRecords.srvRecord(srvRecord.build());
                            propertiesBuilder.srvRecords(srvRecords.build());
                        }
                        default -> throw new IllegalStateException("Unknown record type.");
                    }


                    if (existingRecord.isPresent()) {
                        // patch
                        // TODO: Do we have to make some sort of comparison in case of patch?
                        var azureRecord = existingRecord.get();
                        requestBuilder.etag(azureRecord.getEtag());
                        switch (type) {
                            case A -> {
                                var existingRec = new AzureDnsARecords(azureRecord.getProperties().getARecords().array());
                                // is there a better way than calling build to "copy current state"?
                                var newRec = propertiesBuilder.build().getARecords();
                                var merged = Stream.concat(existingRec.stream(), newRec.stream())
                                    .toList();
                                var records = new AzureDnsARecords.Builder();
                                merged.forEach(records::aRecord);
                                propertiesBuilder.aRecords(records.build());
                            }
                            case TXT -> {
                                var valueSet = new HashSet<String>();
                                var existingRec = new AzureDnsTXTRecords(azureRecord.getProperties().getTXTRecords().array()).getRecords();
                                var newRec = propertiesBuilder.build().getTXTRecords();
                                var merged = Stream.concat(existingRec.stream(), newRec.stream()).toList();
                                // ensure only unique values?
                                merged.stream()
                                    .map(AzureDnsTXTRecord::getValues)
                                    .forEach(valueSet::addAll);
                                // set for unique values in TXT record
                                var records = new AzureDnsTXTRecords.Builder();
                                var txtRecord = new AzureDnsTXTRecord.Builder();
                                valueSet.forEach(txtRecord::value);
                                records.txtRecord(txtRecord.build());
                                propertiesBuilder.txtRecords(records.build());
                            }
                            case SRV -> {
                                var existingRec = new AzureDnsSRVRecords(azureRecord.getProperties().getSRVRecords().array());
                                var newRec = propertiesBuilder.build().getSRVRecords();
                                var merged = Stream.concat(existingRec.stream(), newRec.stream())
                                    .toList();
                                var records = new AzureDnsSRVRecords.Builder();
                                merged.forEach(records::srvRecord);
                                propertiesBuilder.srvRecords(records.build());
                            }
                            default -> throw new IllegalStateException("Unknown record type.");
                        }
                        requestBuilder.properties(propertiesBuilder.build());
                        azureDnsClient.patchRecord(requestBuilder.build(), azureRecord.getName(), type);
                    } else {
                        // put
                        requestBuilder.properties(propertiesBuilder.build());
                        azureDnsClient.putRecord(requestBuilder.build(), name, type);
                    }
                });
    }

    @Override
    public void preDestroy() {
        var ip = instanceConfiguration.getIp() instanceof Inet4Address address ? address : (Inet6Address) instanceConfiguration.getIp();
        // unregister
        var azureDnsClient = loginClient.loginDns()
            .toCompletableFuture()
            .join();

        final var existingRecords = azureDnsClient.listRecords(null)
            .toCompletableFuture()
            .join();


        System.out.println(existingRecords.json().toPrettyString());

        // Don't touch TXT (type 16) records
        dnsRecords.getRecords().stream()
                .filter(r -> r.getType() != 16)
                .forEach(record -> {
                    var name = getNameWithoutZone(record.getName().toString());
                    var existingRecord = existingRecords.list()
                        .stream()
                        .filter(adr ->
                            adr.getName().equals(name) &&
                                compareType(adr.getType(), record.getType()))
                        .findFirst();
                    var type = toType(record.getType());
                    switch (type) {
                        case A -> {
                            if (existingRecord.isPresent()) {
                                var existingRec = existingRecord.get();
                                var elements = existingRec.getProperties().getARecords();
                                if (elements.getRecords().size() == 1 && elements.stream()
                                        .anyMatch(azureDnsARecord -> azureDnsARecord.getIP().equals(ip.getHostAddress()))) {
                                    azureDnsClient.deleteRecord(name, RecordType.A);
                                } else {
                                    var requestBuilder = new AzureDnsRecord.Builder();
                                    var propertiesBuilder = new AzureDnsRecordProperties.Builder();
                                    var aRecords = new AzureDnsARecords.Builder();
                                    elements.stream().filter(azureDnsARecord -> !azureDnsARecord.getIP().equals(ip.getHostAddress()))
                                        .forEach(aRecords::aRecord);
                                    propertiesBuilder.aRecords(aRecords.build());
                                    requestBuilder.properties(propertiesBuilder.build());
                                    azureDnsClient.patchRecord(requestBuilder.build(), name, RecordType.A);
                                }
                            }
                        }
                        case SRV -> {
                            var rec = (SRVRecord) record;
                            if (existingRecord.isPresent()) {
                                var existingRec = existingRecord.get();
                                var elements = existingRec.getProperties().getSRVRecords();
                                if (elements.getRecords().size() == 1 && elements.stream()
                                        .anyMatch(azureDnsSRVRecord -> azureDnsSRVRecord.getTarget().equals(rec.getTarget().toString(true)) && azureDnsSRVRecord.getPort() == rec.getPort())) {
                                    azureDnsClient.deleteRecord(name, RecordType.SRV);
                                } else {
                                    var requestBuilder = new AzureDnsRecord.Builder();
                                    var propertiesBuilder = new AzureDnsRecordProperties.Builder();
                                    var srvRecords = new AzureDnsSRVRecords.Builder();
                                    elements.stream()
                                        .filter(azureDnsSRVRecord -> !azureDnsSRVRecord.getTarget().equals(rec.getTarget().toString(true)) && azureDnsSRVRecord.getPort() != rec.getPort())
                                        .forEach(srvRecords::srvRecord);
                                    propertiesBuilder.srvRecords(srvRecords.build());
                                    requestBuilder.properties(propertiesBuilder.build());
                                    azureDnsClient.patchRecord(requestBuilder.build(), name, RecordType.SRV);
                                }
                            }
                        }
                        default -> throw new IllegalStateException("Unknown record type.");
                    }
                });
    }

    /**
     * Pattern for azure type is "Microsoft.Network/privateDnsZones/A"
     */
    private boolean compareType(String azureType, int dnsType) {
        return (azureType.endsWith("/A") && dnsType == 1) ||
            (azureType.endsWith("/TXT") && dnsType == 16) ||
            (azureType.endsWith("/SRV") && dnsType == 33);
    }

    /**
     * Map DNS types from Java DNS to RecordType
     */
    private RecordType toType(int type) {
        return switch (type) {
            case  1 -> RecordType.A;
            case 16 -> RecordType.TXT;
            case 33 -> RecordType.SRV;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private String getNameWithoutZone(String nameWithDomain) {
        if (nameWithDomain != null && nameWithDomain.endsWith(zoneString)) {
            return nameWithDomain.substring(0, nameWithDomain.length() - zoneString.length());
        }
        return nameWithDomain;
    }
}
