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
package dev.xorcery.dns.update.route53;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.dns.update.spi.DnsUpdateProvider;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


@Service(name = "dns.route53")
@ContractsProvided(DnsUpdateProvider.class)
public class Route53DnsUpdateProvider
        implements DnsUpdateProvider, PreDestroy {
    private final Route53Client client;
    private final Logger logger;

    @Inject
    public Route53DnsUpdateProvider(Configuration configuration, AwsCredentialsProvider awsCredentialsProvider, Logger logger) {
        Route53DnsUpdateConfiguration route53Configuration = Route53DnsUpdateConfiguration.get(configuration);
        this.logger = logger;
        client = Route53Client.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(route53Configuration.getRegion()))
                .build();
    }

    @Override
    public CompletionStage<Void> updateDns(String zone, List<Record> dnsUpdates) {

        try {
            String hostedZoneId = getHostedZoneId(zone);
            ChangeResourceRecordSetsRequest request = ChangeResourceRecordSetsRequest.builder()
                    .hostedZoneId(hostedZoneId)
                    .changeBatch(ChangeBatch.builder()
                            .changes(dnsUpdates.stream().map(Route53DnsUpdateProvider::recordChanges).toList())
                            .build()).build();

            ChangeResourceRecordSetsResponse response = client.changeResourceRecordSets(request);
            logger.info("Updated Route53 {}", response.changeInfo().toString());
            return CompletableFuture.completedFuture(null);

        } catch (AwsServiceException | SdkClientException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private String getHostedZoneId(String zone) {
        ListHostedZonesByNameRequest request = ListHostedZonesByNameRequest.builder()
                .dnsName(zone)
                .build();
        ListHostedZonesByNameResponse response = client.listHostedZonesByName(request);
        String hostedZoneId = response.hostedZones().stream()
                .map(HostedZone::id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find a hosted zone for DNS domain:" + zone));
        // For some reason the id is prefixed with /hostedzone/
        int separatorIdx = hostedZoneId.lastIndexOf('/');
        return separatorIdx == -1 ? hostedZoneId : hostedZoneId.substring(separatorIdx + 1);
    }

    private static Change recordChanges(Record record) {
        return Change.builder()
                .action(record.getDClass() == DClass.IN ? ChangeAction.UPSERT : ChangeAction.DELETE)
                .resourceRecordSet(builder -> builder.name(record.getName().toString())
                        .type(RRType.fromValue(Type.string(record.getType())))
                        .ttl(record.getTTL())
                        .resourceRecords(resourceRecord ->
                        {
                            switch (record.getType()) {
                                case Type.A -> resourceRecord.value(((ARecord) record).getAddress().getHostAddress());
                                case Type.CNAME ->
                                        resourceRecord.value(((CNAMERecord) record).getTarget().toString());
                                case Type.TXT -> resourceRecord.value(((TXTRecord) record).getStrings().stream()
                                        .map(str -> '\"' + str + '\"')
                                        .reduce((str1, str2) -> str1 + " " + str2).orElse(""));
                                case Type.SRV -> {
                                    SRVRecord srvRecord = (SRVRecord) record;
                                    resourceRecord.value(String.format("%d %d %d %s",
                                            srvRecord.getPriority(),
                                            srvRecord.getWeight(),
                                            srvRecord.getPort(),
                                            srvRecord.getTarget().toString()));
                                }
                            }
                        })).build();
    }

    @Override
    public void preDestroy() {
        client.close();
    }
}
