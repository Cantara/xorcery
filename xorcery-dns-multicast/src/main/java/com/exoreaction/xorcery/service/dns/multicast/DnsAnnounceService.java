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
package com.exoreaction.xorcery.service.dns.multicast;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.service.dns.update.spi.DnsUpdateProvider;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TXTRecord;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Service(name = "dns.multicast.announce")
public class DnsAnnounceService
        implements DnsUpdateProvider, PreDestroy {

    private final String selfName;
    private final Logger logger = LogManager.getLogger(getClass());
    private final JmDNS jmdns;
    private final InstanceConfiguration instanceConfiguration;

    @Inject
    public DnsAnnounceService(
            Configuration configuration,
            JmDNS jmDNS) {
        jmdns = jmDNS;
        instanceConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));
        selfName = instanceConfiguration.getId();
    }

    @Override
    public CompletionStage<Void> updateDns(String zone, List<Record> dnsUpdates) {

        Map<String, Map<String, String>> props = new HashMap<>();
        for (Record record : dnsUpdates) {
            if (record instanceof TXTRecord txtRecord) {
                Map<String, String> serviceProps = new HashMap<>();
                for (String string : txtRecord.getStrings()) {
                    String[] keyValue = string.split("=", 2);
                    serviceProps.put(keyValue[0], keyValue[1]);
                }
                props.put(txtRecord.getName().toString(), serviceProps);
            }
        }

        for (Record record : dnsUpdates) {
            if (record instanceof SRVRecord srvRecord) {
                ServiceInfo serviceInfo = ServiceInfo.create(srvRecord.getName().toString(), selfName, instanceConfiguration.getURI().getPort(), srvRecord.getWeight(), srvRecord.getPriority(), props.get(srvRecord.getName().toString()));
                if (srvRecord.getDClass() == DClass.NONE) {
                    jmdns.unregisterService(serviceInfo);

                } else {
                    try {
                        jmdns.registerService(serviceInfo);
                    } catch (IOException e) {
                        jmdns.unregisterAllServices();
                        return CompletableFuture.failedStage(e);
                    }
                }

                logger.debug("Announced mDNS service:" + serviceInfo.getNiceTextString());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void preDestroy() {
        jmdns.unregisterAllServices();
    }
}
