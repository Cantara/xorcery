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
package com.exoreaction.xorcery.dns.registration;

import com.exoreaction.xorcery.dns.update.DnsUpdates;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service(name = "dns.registration")
@RunLevel(20)
public class DnsRegistrationService
        implements PreDestroy {

    private final DnsUpdates dnsUpdates;
    private final Logger logger;
    private final List<Record> records;

    @Inject
    public DnsRegistrationService(DnsRecords dnsRecords,
                                  DnsUpdates dnsUpdates,
                                  Logger logger) throws IOException {
        this.dnsUpdates = dnsUpdates;
        this.logger = logger;
        this.records = dnsRecords.getRecords();

        dnsUpdates.updateDns(records).toCompletableFuture().orTimeout(10, TimeUnit.SECONDS).whenComplete((result, throwable)->
        {
            if (throwable != null)
            {
                logger.error("Could not register with DNS", throwable);
            }
        }).join();
        logger.info("Registered services in DNS");
    }

    @Override
    public void preDestroy() {
        // Remove DNS records for this server
        List<Record> deleteRecords = new ArrayList<>();
        for (Record registeredRecord : records) {
            deleteRecords.add(Record.newRecord(registeredRecord.getName(), registeredRecord.getType(), DClass.NONE, registeredRecord.getTTL(), registeredRecord.rdataToWireCanonical()));
        }

        try {
            dnsUpdates.updateDns(deleteRecords).toCompletableFuture().orTimeout(10, TimeUnit.SECONDS).join();
            logger.info("Deregistered services from DNS");
        } catch (Exception e) {
            logger.error("Failed to deregister services from DNS", e);
        }
    }
}
