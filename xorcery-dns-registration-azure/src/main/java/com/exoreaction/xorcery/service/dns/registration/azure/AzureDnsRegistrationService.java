package com.exoreaction.xorcery.service.dns.registration.azure;

import com.exoreaction.xorcery.configuration.model.Configuration;
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
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TXTRecord;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service(name = "dns.registration.azure")
@RunLevel(20)
public class AzureDnsRegistrationService implements PreDestroy {
    private final Logger logger = LogManager.getLogger(getClass());
    private final AzureConfiguration configuration;
    private final DnsRecords dnsRecords;
    private final AzureAuthenticationClient loginClient;

    @Inject
    public AzureDnsRegistrationService(HttpClient httpClient, Configuration configuration, DnsRecords dnsRecords) {
        this.configuration = () -> configuration.getConfiguration("dns.registration.azure");
        this.dnsRecords = dnsRecords;
        // get required configuration properties
        this.loginClient = new AzureAuthenticationClient(httpClient, this.configuration);
        var azureDnsClient = loginClient.loginDns()
                .toCompletableFuture()
                .join();

        // register
        final var existingRecords = azureDnsClient.listRecords(null)
                .thenApply(azureDnsResponse -> {
                    if (azureDnsResponse.hasNext()) {
                        // do "pagination" in case there's more than 100 records.
                        // azureDnsClient.listRecords(azureDnsResponse.getNextLink())
                    }
                    return azureDnsResponse;
                })
                .toCompletableFuture()
                .join();

        System.out.println(existingRecords.json().toPrettyString());

        dnsRecords.getRecords()
                .forEach(record -> {
                    // I think we need to manually get record name and substring the zone away...
                    var existingRecord = existingRecords.list()
                            .stream()
                            .filter(adr ->
                                adr.getName().equals(record.getName().toString(true)) &&
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
                            aRecord.setIP(rec.getAddress().getHostAddress());
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
                        default -> throw new IllegalStateException("This is bad?");
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
                            default -> throw new IllegalStateException("This is bad?");
                        }
                        requestBuilder.properties(propertiesBuilder.build());
                        azureDnsClient.patchRecord(requestBuilder.build(), azureRecord.getName(), type);
                    } else {
                        // put
                        requestBuilder.properties(propertiesBuilder.build());
                        azureDnsClient.putRecord(requestBuilder.build(), record.getName().toString(true), type);
                    }
                });
    }

    @Override
    public void preDestroy() {
        // unregister
        // get required configuration properties
        var azureDnsClient = loginClient.loginDns()
            .toCompletableFuture()
            .join();

        final var existingRecords = azureDnsClient.listRecords(null)
            .toCompletableFuture()
            .join();


        System.out.println(existingRecords.json().toPrettyString());

        // Don't touch A (type 1) records
        dnsRecords.getRecords().stream()
                .filter(r -> r.getType() != 1)
                .forEach(record -> {
                    // TODO: add removal / patch logic
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
}
