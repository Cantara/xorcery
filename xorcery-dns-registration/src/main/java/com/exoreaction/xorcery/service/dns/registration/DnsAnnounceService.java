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
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TXTRecord;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service(name = "dns.registration.announce")
@RunLevel(20)
public class DnsAnnounceService
        implements PreDestroy {

    private final String selfName;
    private final Logger logger = LogManager.getLogger(getClass());
    private final JmDNS jmdns;

    @Inject
    public DnsAnnounceService(
            Configuration configuration,
            DnsRecords dnsRecords,
            JmDNS jmDNS) throws IOException {
        jmdns = jmDNS;
        selfName = configuration.getString("id").orElse("xorcery");
        InstanceConfiguration standardConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));

        Map<String, Map<String, String>> props = new HashMap<>();
        for (Record record : dnsRecords.getRecords()) {
            if (record instanceof TXTRecord txtRecord)
            {
                Map<String, String> serviceProps = new HashMap<>();
                for (String string : txtRecord.getStrings()) {
                    String[] keyValue = string.split("=", 2);
                    serviceProps.put(keyValue[0], keyValue[1]);
                }
                props.put(txtRecord.getName().toString(), serviceProps);
            }
        }

        for (Record record : dnsRecords.getRecords()) {
            if (record instanceof SRVRecord srvRecord)
            {
                ServiceInfo serviceInfo = ServiceInfo.create(srvRecord.getName().toString(), selfName, standardConfiguration.getURI().getPort(), srvRecord.getWeight(), srvRecord.getPriority(), props.get(srvRecord.getName().toString()));
                jmdns.registerService(serviceInfo);
                logger.debug("Announced mDNS service:" + serviceInfo.getNiceTextString());
            }
        }
    }

    @Override
    public void preDestroy() {
        jmdns.unregisterAllServices();
    }
}
